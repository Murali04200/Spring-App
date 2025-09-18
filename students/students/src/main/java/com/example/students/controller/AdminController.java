package com.example.students.controller;

import com.example.students.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final StudentService studentService;

    public AdminController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Admin Dashboard
    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin-dashboard";
    }

    //Admin Student List
    @GetMapping("/admin/students")
    public String listStudentsForAdmin(Model model) {
        model.addAttribute("students", studentService.findAll());
        return "students"; // reuse students.html
    }
}
