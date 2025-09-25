package com.example.students.service;

import com.example.students.model.Student;
import com.example.students.repo.StudentRepository;
import com.example.students.repo.SubjectMarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {
    private final StudentRepository repository;
    private final SubjectMarkRepository subjectMarkRepository;

    public StudentService(StudentRepository repository, SubjectMarkRepository subjectMarkRepository) {
        this.repository = repository;
        this.subjectMarkRepository = subjectMarkRepository;
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

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
}
