package com.example.students.service;

import com.example.students.model.*;
import com.example.students.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectMarkService {

    private final SubjectMarkRepository subjectMarkRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public SubjectMarkService(SubjectMarkRepository subjectMarkRepository,
                              StudentRepository studentRepository,
                              SubjectRepository subjectRepository,
                              TeacherRepository teacherRepository) {
        this.subjectMarkRepository = subjectMarkRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional
    public SubjectMark saveOrUpdateMark(Long studentId, String subjectCode, String teacherEmail, Integer mark) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Subject subject = subjectRepository.findByCode(subjectCode)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectCode));
        Teacher teacher = teacherRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherEmail));

        // Validate that teacher teaches the selected subject
        if (teacher.getSubjectCode() == null || !teacher.getSubjectCode().equalsIgnoreCase(subject.getCode())) {
            throw new IllegalArgumentException("Teacher is not assigned to subject: " + subject.getCode());
        }

        SubjectMark sm = subjectMarkRepository.findByStudentAndSubject(student, subject)
                .orElseGet(SubjectMark::new);
        sm.setStudent(student);
        sm.setSubject(subject);
        sm.setTeacher(teacher);
        sm.setMark(mark);

        SubjectMark saved = subjectMarkRepository.save(sm);

        // Recompute and persist total marks on Student
        recomputeAndPersistTotal(student);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SubjectMark> getMarksForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        return subjectMarkRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public List<SubjectMark> getMarksForTeacherSubject(String subjectCode, String teacherEmail) {
        return subjectMarkRepository.findBySubject_CodeAndTeacher_Email(subjectCode, teacherEmail);
    }

    @Transactional
    public void recomputeAndPersistTotal(Student student) {
        List<SubjectMark> marks = subjectMarkRepository.findByStudent(student);
        // Only compute total after 5 subject marks are present; else keep it null
        if (marks.size() >= 5) {
            int total = marks.stream().mapToInt(m -> m.getMark() == null ? 0 : m.getMark()).sum();
            student.setTotalMarks(total);
        } else {
            student.setTotalMarks(null);
        }
        studentRepository.save(student);
    }
}
