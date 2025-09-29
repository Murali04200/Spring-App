package com.example.students.repo;

import com.example.students.model.SubjectMark;
import com.example.students.model.Student;
import com.example.students.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectMarkRepository extends JpaRepository<SubjectMark, Long> {
    List<SubjectMark> findByStudent(Student student);
    Optional<SubjectMark> findByStudentAndSubject(Student student, Subject subject);

    @Modifying
    @Query("delete from SubjectMark sm where sm.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("delete from SubjectMark sm where sm.teacher.id = :teacherId")
    void deleteByTeacherId(@Param("teacherId") Long teacherId);

    // Fetch marks for a given subject code and teacher email
    List<SubjectMark> findBySubject_CodeAndTeacher_Email(String subjectCode, String email);
}
