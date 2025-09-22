package com.example.students.controller;

import com.example.students.model.Student;
import com.example.students.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ✅ Show all students
    @GetMapping
    public String list(Model model) {
        model.addAttribute("students", service.findAll());
        return "students";
    }

    // ✅ Show create form (Admin only)
    @GetMapping("/new")
    @PreAuthorize("hasRole('admin')")
    public String createForm(Model model) {
        model.addAttribute("student", new Student());
        return "student-form";
    }

    // ✅ Save new student (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public String save(@ModelAttribute Student student) {
        service.save(student);
        return "redirect:/students";
    }

    // ✅ Delete student (Admin only)
    @PreAuthorize("hasRole('admin')")
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteById(id);
        return "redirect:/students";
    }

    // ✅ Handle update (Admin, Manager, JrManager, Clerk)
    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('admin','manager','jrmanager','clerk')")
    public String update(@PathVariable Long id, @ModelAttribute Student student) {
        service.updateStudent(id, student);
        return "redirect:/students";
    }
}
