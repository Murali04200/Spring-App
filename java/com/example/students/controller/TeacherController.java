package com.example.students.controller;

import com.example.students.service.SubjectMarkService;
import com.example.students.repo.StudentRepository;
import com.example.students.repo.SubjectRepository;
import com.example.students.repo.TeacherRepository;
import com.example.students.repo.TeacherAssignmentRepository;
import com.example.students.model.TeacherAssignment;
import com.example.students.repo.KeycloakUserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.students.annotation.RequirePermission;
import com.example.students.model.Teacher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final SubjectMarkService subjectMarkService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final KeycloakUserRepository keycloakUserRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;

    public TeacherController(SubjectMarkService subjectMarkService,
                             StudentRepository studentRepository,
                             SubjectRepository subjectRepository,
                             TeacherRepository teacherRepository,
                             KeycloakUserRepository keycloakUserRepository,
                             TeacherAssignmentRepository teacherAssignmentRepository) {
        this.subjectMarkService = subjectMarkService;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.keycloakUserRepository = keycloakUserRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
    }

    // Render mark entry form
    @GetMapping("/marks")
    @RequirePermission("I")
    public String showMarkForm(Model model, Authentication authentication) {
        model.addAttribute("students", studentRepository.findAll());

        // Determine logged-in email
        String email = null;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidc) {
                email = oidc.getEmail();
                if (email == null || email.isBlank()) {
                    Object attr = oidc.getAttributes().get("email");
                    email = attr != null ? String.valueOf(attr) : null;
                }
            } else {
                email = authentication.getName();
            }
        }

        // Find teacher by email; if present, lock subject/teacher to their assignment
        if (email != null) {
            var teacherOpt = teacherRepository.findByEmail(email);
            if (teacherOpt.isPresent()) {
                Teacher t = teacherOpt.get();
                model.addAttribute("fixedSubjectCode", t.getSubjectCode());
                model.addAttribute("fixedTeacherEmail", t.getEmail());
                model.addAttribute("subjects", subjectRepository.findByCode(t.getSubjectCode()).stream().toList());
                model.addAttribute("teachers", teacherRepository.findAllBySubjectCode(t.getSubjectCode()));
                // Load teacher's marks for their subject to display in table
                model.addAttribute("teacherMarks", subjectMarkService.getMarksForTeacherSubject(t.getSubjectCode(), t.getEmail()));
                return "teacher-mark-entry";
            }
        }

        // Fallback: no teacher record found for this user, show all
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("teacherMarks", java.util.List.of());
        return "teacher-mark-entry";
    }

    // Handle submission
    @PostMapping("/marks")
    @RequirePermission("I")
    public String submitMark(@RequestParam Long studentId,
                             @RequestParam String subjectCode,
                             @RequestParam String teacherEmail,
                             @RequestParam Integer mark,
                             @RequestParam(value = "redirectTo", required = false) String redirectTo,
                             Model model,
                             Authentication authentication) {
        // Enforce that the logged-in teacher can only submit for their subject
        String email = teacherEmail;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidc) {
                String em = oidc.getEmail();
                if (em == null || em.isBlank()) {
                    Object attr = oidc.getAttributes().get("email");
                    em = attr != null ? String.valueOf(attr) : null;
                }
                if (em != null) {
                    email = em; // prefer logged-in email over submitted value
                }
            }
        }

        var tOpt = teacherRepository.findByEmail(email);
        if (tOpt.isPresent()) {
            Teacher t = tOpt.get();
            if (!t.getSubjectCode().equalsIgnoreCase(subjectCode)) {
                throw new IllegalArgumentException("You are only allowed to enter marks for subject: " + t.getSubjectCode());
            }
            // Force teacherEmail to logged-in teacher
            teacherEmail = t.getEmail();
            subjectCode = t.getSubjectCode();
        }

        subjectMarkService.saveOrUpdateMark(studentId, subjectCode, teacherEmail, mark);
        model.addAttribute("message", "Mark saved successfully");
        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/teacher/marks";
    }

    // Create Teacher form
    @GetMapping("/new")
    @RequirePermission("H")
    public String createTeacherForm(Model model) {
        model.addAttribute("teacher", new Teacher());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("kcTeachers", keycloakUserRepository.findAllByRoleIgnoreCase("teacher"));
        return "teacher-form";
    }

    // Save Teacher
    @PostMapping
    @RequirePermission("H")
    public String saveTeacher(@ModelAttribute Teacher teacher,
                              @RequestParam(value = "newSubjectCode", required = false) String newSubjectCode,
                              @RequestParam(value = "newSubjectName", required = false) String newSubjectName) {
        // Resolve subject code either from existing dropdown or newly created subject
        String resolvedSubjectCode = teacher.getSubjectCode();
        if (newSubjectCode != null && !newSubjectCode.isBlank()) {
            String code = newSubjectCode.trim();
            java.util.Optional<com.example.students.model.Subject> existing = subjectRepository.findByCode(code);
            if (existing.isEmpty()) {
                if (newSubjectName == null || newSubjectName.isBlank()) {
                    throw new IllegalArgumentException("Subject name is required when creating a new subject code: " + code);
                }
                com.example.students.model.Subject s = new com.example.students.model.Subject();
                s.setCode(code);
                s.setName(newSubjectName.trim());
                subjectRepository.save(s);
            }
            resolvedSubjectCode = code;
        } else {
            // Validate that the selected subject exists (avoid lambda capture)
            java.util.Optional<com.example.students.model.Subject> sel = subjectRepository.findByCode(resolvedSubjectCode);
            if (sel.isEmpty()) {
                throw new IllegalArgumentException("Invalid subject code: " + resolvedSubjectCode);
            }
        }

        // Persist teacher with resolved subject code
        teacher.setSubjectCode(resolvedSubjectCode);
        teacherRepository.save(teacher);

        // Create TeacherAssignment entries for each selected class (stored as CSV in department)
        try {
            var subjectOpt = subjectRepository.findByCode(resolvedSubjectCode);
            if (subjectOpt.isPresent()) {
                var subject = subjectOpt.get();
                String classesCsv = teacher.getDepartment(); // contains e.g. "8,9,10"
                if (classesCsv != null && !classesCsv.isBlank()) {
                    for (String cls : classesCsv.split(",")) {
                        String className = cls.trim();
                        if (className.isEmpty()) continue;
                        var existing = teacherAssignmentRepository
                                .findByTeacherAndSubjectAndClassName(teacher, subject, className);
                        if (existing.isEmpty()) {
                            TeacherAssignment ta = new TeacherAssignment();
                            ta.setTeacher(teacher);
                            ta.setSubject(subject);
                            ta.setClassName(className);
                            teacherAssignmentRepository.save(ta);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "redirect:/teacher/new?success";
    }
}
