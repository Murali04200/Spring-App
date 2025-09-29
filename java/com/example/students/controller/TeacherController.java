package com.example.students.controller;

import com.example.students.annotation.RequirePermission;
import com.example.students.model.Teacher;
import com.example.students.model.TeacherAssignment;
import com.example.students.repo.KeycloakUserRepository;
import com.example.students.repo.StudentRepository;
import com.example.students.repo.SubjectRepository;
import com.example.students.repo.TeacherAssignmentRepository;
import com.example.students.repo.TeacherRepository;
import com.example.students.service.TeacherService;
import com.example.students.service.SubjectMarkService;
import com.example.students.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final SubjectMarkService subjectMarkService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final KeycloakUserRepository keycloakUserRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final NotificationService notificationService;
    private final TeacherService teacherService;

    public TeacherController(SubjectMarkService subjectMarkService,
                             StudentRepository studentRepository,
                             SubjectRepository subjectRepository,
                             TeacherRepository teacherRepository,
                             KeycloakUserRepository keycloakUserRepository,
                             TeacherAssignmentRepository teacherAssignmentRepository,
                             NotificationService notificationService,
                             TeacherService teacherService) {
        this.subjectMarkService = subjectMarkService;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.keycloakUserRepository = keycloakUserRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.notificationService = notificationService;
        this.teacherService = teacherService;
    }

    // Render mark entry form
    @GetMapping("/marks")
    @RequirePermission("I")
    public String showMarkForm(Model model, Authentication authentication) {
        model.addAttribute("students", studentRepository.findAll());

        String email = resolveEmail(authentication);

        if (email != null) {
            var teacherOpt = teacherRepository.findByEmail(email);
            if (teacherOpt.isPresent()) {
                Teacher t = teacherOpt.get();
                model.addAttribute("fixedSubjectCode", t.getSubjectCode());
                model.addAttribute("fixedTeacherEmail", t.getEmail());
                model.addAttribute("subjects", subjectRepository.findByCode(t.getSubjectCode()).stream().toList());
                model.addAttribute("teachers", teacherRepository.findAllBySubjectCode(t.getSubjectCode()));
                model.addAttribute("teacherMarks", subjectMarkService.getMarksForTeacherSubject(t.getSubjectCode(), t.getEmail()));
                model.addAttribute("recentMarks", subjectMarkService.recentMarksForTeacher(t.getSubjectCode(), t.getEmail(), 10));
                return "teacher-mark-entry";
            }
        }

        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("teacherMarks", List.of());
        model.addAttribute("recentMarks", subjectMarkService.recentMarks(10));
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
        String email = resolveEmail(authentication);
        if (email != null) {
            teacherEmail = email;
            var tOpt = teacherRepository.findByEmail(email);
            if (tOpt.isPresent()) {
                Teacher t = tOpt.get();
                if (!t.getSubjectCode().equalsIgnoreCase(subjectCode)) {
                    throw new IllegalArgumentException("You are only allowed to enter marks for subject: " + t.getSubjectCode());
                }
                subjectCode = t.getSubjectCode();
            }
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
        String resolvedSubjectCode = teacher.getSubjectCode();
        if (newSubjectCode != null && !newSubjectCode.isBlank()) {
            String code = newSubjectCode.trim();
            var existing = subjectRepository.findByCode(code);
            if (existing.isEmpty()) {
                if (newSubjectName == null || newSubjectName.isBlank()) {
                    throw new IllegalArgumentException("Subject name is required when creating a new subject code: " + code);
                }
                var subject = new com.example.students.model.Subject();
                subject.setCode(code);
                subject.setName(newSubjectName.trim());
                subjectRepository.save(subject);
            }
            resolvedSubjectCode = code;
        } else {
            var sel = subjectRepository.findByCode(resolvedSubjectCode);
            if (sel.isEmpty()) {
                throw new IllegalArgumentException("Invalid subject code: " + resolvedSubjectCode);
            }
        }

        teacher.setSubjectCode(resolvedSubjectCode);
        Teacher savedTeacher = teacherRepository.save(teacher);

        try {
            var subjectOpt = subjectRepository.findByCode(resolvedSubjectCode);
            if (subjectOpt.isPresent()) {
                var subject = subjectOpt.get();
                String classesCsv = teacher.getDepartment();
                if (classesCsv != null && !classesCsv.isBlank()) {
                    for (String cls : classesCsv.split(",")) {
                        String className = cls.trim();
                        if (className.isEmpty()) continue;
                        var existing = teacherAssignmentRepository.findByTeacherAndSubjectAndClassName(teacher, subject, className);
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
        notificationService.sendTeacherWelcome(savedTeacher);
        return "redirect:/teacher/new?success";
    }

    // View Teacher Details (any authenticated user)
    @GetMapping("/details/{id}")
    public String teacherDetails(@PathVariable Long id, Model model, Authentication authentication) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + id));
        populateTeacherDetails(model, teacher, authentication);
        return "teacher-details";
    }

    // Resolve current user's teacher details
    @GetMapping("/details/me")
    public String myTeacherDetails(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        String email = resolveEmail(authentication);
        if (email != null) {
            var tOpt = teacherRepository.findByEmail(email);
            if (tOpt.isPresent()) {
                populateTeacherDetails(model, tOpt.get(), authentication);
                return "teacher-details";
            }
        }
        model.addAttribute("hasTeacher", false);
        model.addAttribute("classOptions", defaultClassOptions());
        model.addAttribute("canCreate", hasAuthority(authentication, "PERM_H"));
        return "teacher-details";
    }

    @PostMapping("/details/{id}/update")
    public String updateTeacherDetails(@PathVariable Long id,
                                       @RequestParam String name,
                                       @RequestParam(value = "classSelections", required = false) String classSelections,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + id));

        boolean isOwner = teacher.getEmail() != null && teacher.getEmail().equalsIgnoreCase(resolveEmail(authentication));
        if (!isOwner && !hasAuthority(authentication, "PERM_H")) {
            redirectAttributes.addFlashAttribute("error", "You are not allowed to update this teacher profile.");
            return "redirect:/teacher/details/" + id;
        }

        teacher.setName(name.trim());

        List<String> newClasses = new ArrayList<>();
        if (classSelections != null && !classSelections.isBlank()) {
            for (String cls : classSelections.split(",")) {
                String trimmed = cls.trim();
                if (!trimmed.isEmpty()) newClasses.add(trimmed);
            }
        }
        newClasses.sort(String::compareTo);
        teacher.setDepartment(String.join(",", newClasses));
        teacherRepository.save(teacher);

        var subjectOpt = subjectRepository.findByCode(teacher.getSubjectCode());
        subjectOpt.ifPresent(subject -> syncAssignments(teacher, subject, newClasses));

        redirectAttributes.addFlashAttribute("updated", true);
        return "redirect:/teacher/details/" + id;
    }

    // Admin update endpoint
    @PostMapping("/{id}/update")
    @RequirePermission("H")
    public String adminUpdateTeacher(@PathVariable Long id,
                                     @RequestParam String name,
                                     @RequestParam String subjectCode,
                                     @RequestParam String email,
                                     @RequestParam String department,
                                     RedirectAttributes redirectAttributes) {
        try {
            teacherService.updateTeacher(id, name, subjectCode, email, department);
            redirectAttributes.addFlashAttribute("teacherUpdated", true);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("teacherError", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // Admin delete endpoint
    @PostMapping("/{id}/delete")
    @RequirePermission("H")
    public String adminDeleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean removed = teacherService.deleteTeacher(id);
        if (removed) {
            redirectAttributes.addFlashAttribute("teacherDeleted", true);
        } else {
            redirectAttributes.addFlashAttribute("teacherError", "Teacher not found or already deleted.");
        }
        return "redirect:/admin/dashboard";
    }

    private void populateTeacherDetails(Model model, Teacher teacher, Authentication authentication) {
        model.addAttribute("hasTeacher", true);
        model.addAttribute("teacher", teacher);

        var subjOpt = subjectRepository.findByCode(teacher.getSubjectCode());
        String subjectName = subjOpt.map(s -> s.getName() + " (" + s.getCode() + ")").orElse(teacher.getSubjectCode());
        model.addAttribute("subjectName", subjectName);

        List<String> classes = new ArrayList<>();
        var assignments = teacherAssignmentRepository.findByTeacher(teacher);
        for (var a : assignments) {
            if (a.getClassName() != null && !a.getClassName().isBlank()) classes.add(a.getClassName());
        }
        if (classes.isEmpty() && teacher.getDepartment() != null) {
            for (String cls : teacher.getDepartment().split(",")) {
                String trimmed = cls.trim();
                if (!trimmed.isEmpty()) classes.add(trimmed);
            }
        }
        classes.sort(String::compareTo);
        model.addAttribute("classes", classes);
        model.addAttribute("classOptions", defaultClassOptions());

        boolean canEdit = hasAuthority(authentication, "PERM_H")
                || (teacher.getEmail() != null && resolveEmail(authentication) != null
                && teacher.getEmail().equalsIgnoreCase(resolveEmail(authentication)));
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("recentMarks", subjectMarkService.recentMarksForTeacher(teacher.getSubjectCode(), teacher.getEmail(), 10));
    }

    private void syncAssignments(Teacher teacher, com.example.students.model.Subject subject, List<String> desiredClasses) {
        Set<String> desired = new HashSet<>(desiredClasses);
        var existingAssignments = teacherAssignmentRepository.findByTeacher(teacher);

        for (var assignment : existingAssignments) {
            if (!desired.contains(assignment.getClassName())) {
                teacherAssignmentRepository.delete(assignment);
            }
        }

        Set<String> current = new HashSet<>();
        existingAssignments.forEach(a -> current.add(a.getClassName()));

        for (String cls : desired) {
            if (!current.contains(cls)) {
                TeacherAssignment ta = new TeacherAssignment();
                ta.setTeacher(teacher);
                ta.setSubject(subject);
                ta.setClassName(cls);
                teacherAssignmentRepository.save(ta);
            }
        }
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            if (email == null || email.isBlank()) {
                Object attr = oidc.getAttributes().get("email");
                email = attr != null ? String.valueOf(attr) : null;
            }
            return email;
        }
        return authentication.getName();
    }

    private List<String> defaultClassOptions() {
        return List.of("5", "6", "7", "8", "9", "10", "11", "12");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) return false;
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (authority.equalsIgnoreCase(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
