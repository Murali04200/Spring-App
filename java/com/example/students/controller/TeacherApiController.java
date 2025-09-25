package com.example.students.controller;

import com.example.students.model.Teacher;
import com.example.students.repo.TeacherRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherApiController {

    private final TeacherRepository teacherRepository;

    public TeacherApiController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @GetMapping
    public List<Teacher> all(@RequestParam(value = "subjectCode", required = false) String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank()) {
            return teacherRepository.findAll();
        }
        return teacherRepository.findAllBySubjectCode(subjectCode);
    }
}
