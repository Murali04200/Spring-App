package com.example.students.controller;

import com.example.students.model.Subject;
import com.example.students.repo.SubjectRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectApiController {

    private final SubjectRepository subjectRepository;

    public SubjectApiController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    public List<Subject> all() {
        return subjectRepository.findAll();
    }
}
