package com.example.students.controller;

import com.example.students.service.StudentService;
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

    public AdminController(StudentService studentService,
                           KeycloakUserRepository keycloakUserRepository) {
        this.studentService = studentService;
        this.keycloakUserRepository = keycloakUserRepository;
    }

    // Admin Dashboard
    @GetMapping("/admin/dashboard")
    @RequirePermission(anyOf = {"A","B","C","D","E"})
    public String dashboard(Model model) {
        // Fetch all Keycloak users from DB
        List<KeycloakUser> users = keycloakUserRepository.findAll(); // ✅ use model.KeycloakUser
        model.addAttribute("users", users);

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
