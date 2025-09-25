package com.example.students.repo;

import com.example.students.model.TeacherAssignment;
import com.example.students.model.Subject;
import com.example.students.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    List<TeacherAssignment> findByTeacher(Teacher teacher);
    List<TeacherAssignment> findBySubject(Subject subject);
    Optional<TeacherAssignment> findByTeacherAndSubject(Teacher teacher, Subject subject);
    Optional<TeacherAssignment> findByTeacherAndSubjectAndClassName(Teacher teacher, Subject subject, String className);
}
