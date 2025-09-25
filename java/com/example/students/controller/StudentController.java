package com.example.students.controller;

import com.example.students.model.Student;
import com.example.students.service.StudentService;
import com.example.students.annotation.RequirePermission;
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

    // ✅ Show create form - requires CREATE_USER permission (A)
    @GetMapping("/new")
    @RequirePermission("A")
    public String createForm(Model model) {
        model.addAttribute("student", new Student());
        return "student-form";
    }

    // ✅ Save new student - requires CREATE_USER permission (A)
    @PostMapping
    @RequirePermission("A")
    public String save(@ModelAttribute Student student) {
        service.save(student);
        return "redirect:/students";
    }

    // ✅ Delete student - requires DELETE_USER permission (B)
    @GetMapping("/delete/{id}")
    @RequirePermission("B")
    public String delete(@PathVariable Long id) {
        service.deleteById(id);
        return "redirect:/students";
    }

    // Also support DELETE requests from JS if used
    @DeleteMapping("/delete/{id}")
    @RequirePermission("B")
    public String deleteViaDelete(@PathVariable Long id) {
        service.deleteById(id);
        return "redirect:/students";
    }

    // Support POST deletes (CSRF-friendly from forms)
    @PostMapping("/delete/{id}")
    @RequirePermission("B")
    @ResponseBody
    public org.springframework.http.ResponseEntity<Void> deleteViaPost(@PathVariable Long id) {
        service.deleteById(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    // ✅ Show update form - requires UPDATE_USER permission (C)
    @GetMapping("/edit/{id}")
    @RequirePermission("C")
    public String editForm(@PathVariable Long id, Model model) {
        Student student = service.findById(id); // Make sure findById exists in service
        model.addAttribute("student", student);
        return "student-edit-form"; // Or use modal integration
    }

    // ✅ Handle update - requires UPDATE_USER permission (C)
    @PostMapping("/update/{id}")
    @RequirePermission("C")
    public String update(@PathVariable Long id, @ModelAttribute Student student) {
        service.updateStudent(id, student);
        return "redirect:/students";
    }

    // ✅ Print page for a single student (read-only, any authenticated user)
    @GetMapping("/print/{id}")
    public String print(@PathVariable Long id, Model model) {
        Student student = service.findById(id);
        model.addAttribute("student", student);
        return "student-print";
    }
}
