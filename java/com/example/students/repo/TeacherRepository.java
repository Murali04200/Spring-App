package com.example.students.repo;

import com.example.students.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmail(String email);
    Optional<Teacher> findBySubjectCode(String subjectCode);
    List<Teacher> findAllBySubjectCode(String subjectCode);
}
