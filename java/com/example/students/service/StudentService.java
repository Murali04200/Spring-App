package com.example.students.service;

import com.example.students.model.Student;
import com.example.students.model.Subject;
import com.example.students.model.SubjectMark;
import com.example.students.model.Teacher;
import com.example.students.repo.StudentRepository;
import com.example.students.repo.SubjectMarkRepository;
import com.example.students.repo.SubjectRepository;
import com.example.students.repo.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {
    private final StudentRepository repository;
    private final SubjectMarkRepository subjectMarkRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public StudentService(StudentRepository repository,
                          SubjectMarkRepository subjectMarkRepository,
                          SubjectRepository subjectRepository,
                          TeacherRepository teacherRepository) {
        this.repository = repository;
        this.subjectMarkRepository = subjectMarkRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

    public long countStudents() { return repository.count(); }

    public Student save(Student student) {
        return repository.save(student);
    }

    // ✅ Alias for clarity (used in KeycloakService)
    public Student saveStudent(Student student) {
        return repository.save(student);
    }

    @Transactional
    public void deleteById(Long id) {
        // Delete dependent rows first to avoid FK constraint violations
        subjectMarkRepository.deleteByStudentId(id);
        // Then delete the student if present
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            // No-op if already removed; callers that need strict behavior can change this
        }
    }

    // ✅ Find by ID (for edit/update use case)
    public Student findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    // ✅ Update Student (DB only)
    public Student updateStudent(Long id, Student updatedStudent) {
        Optional<Student> existingOpt = repository.findById(id);

        if (existingOpt.isPresent()) {
            Student existing = existingOpt.get();
            // Update only editable fields
            existing.setName(updatedStudent.getName());
            existing.setEmail(updatedStudent.getEmail());
            existing.setClassName(updatedStudent.getClassName());
            return repository.save(existing);
        } else {
            throw new RuntimeException("Student not found with id: " + id);
        }
    }

    public double averageTotalMarks() {
        Double avg = repository.averageTotalMarks();
        return avg != null ? avg : 0.0;
    }

    public List<ClassDistribution> classDistribution() {
        return repository.countStudentsByClass().stream()
                .map(row -> new ClassDistribution((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<SubjectMark> getSubjectMarksForStudent(Long studentId) {
        Student student = findById(studentId);
        return subjectMarkRepository.findByStudent(student);
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public record ClassDistribution(String className, long count) {}
}
