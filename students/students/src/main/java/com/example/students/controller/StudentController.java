package com.example.students.controller;

import com.example.students.model.Student;
import com.example.students.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("students", service.findAll());
        return "students";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("student", new Student());
        return "student-form";
    }

    @PostMapping
    public String save(@ModelAttribute Student student) {
        service.save(student);
        return "redirect:/students";
    }
}
