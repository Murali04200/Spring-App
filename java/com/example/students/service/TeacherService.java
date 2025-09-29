package com.example.students.service;

import com.example.students.model.Subject;
import com.example.students.model.Teacher;
import com.example.students.model.TeacherAssignment;
import com.example.students.repo.SubjectRepository;
import com.example.students.repo.SubjectMarkRepository;
import com.example.students.repo.TeacherAssignmentRepository;
import com.example.students.repo.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final SubjectMarkRepository subjectMarkRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          SubjectRepository subjectRepository,
                          TeacherAssignmentRepository teacherAssignmentRepository,
                          SubjectMarkRepository subjectMarkRepository) {
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.subjectMarkRepository = subjectMarkRepository;
    }

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    public long count() {
        return teacherRepository.count();
    }

    public Teacher updateTeacher(Long id,
                                 String name,
                                 String subjectCode,
                                 String email,
                                 String departmentCsv) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + id));

        teacher.setName(name.trim());
        String normalizedSubject = subjectCode.trim();
        Subject subject = subjectRepository.findByCode(normalizedSubject)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + normalizedSubject));

        teacher.setSubjectCode(normalizedSubject);
        teacher.setEmail(email.trim());
        teacher.setDepartment(departmentCsv == null ? "" : departmentCsv.trim());
        Teacher saved = teacherRepository.save(teacher);

        syncAssignments(saved, subject, parseClasses(saved.getDepartment()));

        return saved;
    }

    public boolean deleteTeacher(Long id) {
        return teacherRepository.findById(id)
                .map(teacher -> {
                    subjectMarkRepository.deleteByTeacherId(teacher.getId());
                    teacherAssignmentRepository.deleteByTeacher(teacher);
                    teacherRepository.delete(teacher);
                    return true;
                })
                .orElse(false);
    }

    private List<String> parseClasses(String departmentCsv) {
        if (departmentCsv == null || departmentCsv.isBlank()) {
            return List.of();
        }
        List<String> classes = new ArrayList<>();
        for (String cls : departmentCsv.split(",")) {
            String trimmed = cls.trim();
            if (!trimmed.isEmpty()) {
                classes.add(trimmed);
            }
        }
        classes.sort(String::compareTo);
        return classes;
    }

    private void syncAssignments(Teacher teacher, Subject subject, List<String> desiredClasses) {
        Set<String> desired = new HashSet<>(desiredClasses);
        var existingAssignments = teacherAssignmentRepository.findByTeacher(teacher);

        for (TeacherAssignment assignment : existingAssignments) {
            if (!desired.contains(assignment.getClassName())) {
                teacherAssignmentRepository.delete(assignment);
            }
        }

        Set<String> current = new HashSet<>();
        existingAssignments.forEach(a -> current.add(a.getClassName()));

        for (String cls : desired) {
            if (!current.contains(cls)) {
                TeacherAssignment ta = new TeacherAssignment();
                ta.setTeacher(teacher);
                ta.setSubject(subject);
                ta.setClassName(cls);
                teacherAssignmentRepository.save(ta);
            }
        }
    }
}
