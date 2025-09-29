package com.example.students.controller;

import com.example.students.service.StudentService;
import com.example.students.service.SubjectMarkService;
import com.example.students.service.TeacherService;
import com.example.students.model.KeycloakUser;   // ✅ fixed import
import com.example.students.repo.KeycloakUserRepository; // ✅ fixed repo package
import com.example.students.annotation.RequirePermission;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    private final StudentService studentService;
    private final KeycloakUserRepository keycloakUserRepository;
    private final TeacherService teacherService;
    private final SubjectMarkService subjectMarkService;

    public AdminController(StudentService studentService,
                           KeycloakUserRepository keycloakUserRepository,
                           SubjectMarkService subjectMarkService,
                           TeacherService teacherService) {
        this.studentService = studentService;
        this.keycloakUserRepository = keycloakUserRepository;
        this.subjectMarkService = subjectMarkService;
        this.teacherService = teacherService;
    }

    // Admin Dashboard - accessible to any authenticated user; cards are permission-gated in the view
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        // Fetch all Keycloak users from DB
        List<KeycloakUser> users = keycloakUserRepository.findAll(); // ✅ use model.KeycloakUser
        model.addAttribute("users", users);

        model.addAttribute("studentCount", studentService.countStudents());
        model.addAttribute("teacherCount", teacherService.count());
        model.addAttribute("marksCount", subjectMarkService.totalMarksCount());
        model.addAttribute("marksBySubject", subjectMarkService.marksCountBySubject());
        model.addAttribute("recentMarks", subjectMarkService.recentMarks(5));
        model.addAttribute("teachers", teacherService.findAll());

        return "admin-dashboard"; // thymeleaf page
    }

    // Admin Student List
    @GetMapping("/admin/students")
    @RequirePermission(anyOf = {"A","B","C","D","E"})
    public String listStudentsForAdmin(Model model) {
        model.addAttribute("students", studentService.findAll());
        return "students"; // reuse students.html
    }

    // User Dashboard
    @GetMapping("/user/dashboard")
    public String userDashboard() {
        return "user-dashboard";
    }
}
