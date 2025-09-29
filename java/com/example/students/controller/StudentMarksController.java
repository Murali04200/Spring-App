package com.example.students.controller;

import com.example.students.annotation.RequirePermission;
import com.example.students.model.Student;
import com.example.students.model.SubjectMark;
import com.example.students.repo.SubjectRepository;
import com.example.students.repo.TeacherRepository;
import com.example.students.service.SubjectMarkService;
import com.example.students.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/students")
public class StudentMarksController {

    private final SubjectMarkService subjectMarkService;
    private final StudentService studentService;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public StudentMarksController(SubjectMarkService subjectMarkService,
                                  StudentService studentService,
                                  SubjectRepository subjectRepository,
                                  TeacherRepository teacherRepository) {
        this.subjectMarkService = subjectMarkService;
        this.studentService = studentService;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping("/{id}/details")
    @RequirePermission("E")
    public String details(@PathVariable Long id, Model model) {
        Student student = studentService.findById(id);
        List<SubjectMark> marks = subjectMarkService.getMarksForStudent(id);
        model.addAttribute("student", student);
        model.addAttribute("marks", marks);
        // for modal dropdowns
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        return "student-details";
    }
}
