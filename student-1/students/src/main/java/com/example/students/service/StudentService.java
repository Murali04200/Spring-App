package com.example.students.service;

import com.example.students.model.Student;
import com.example.students.repo.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {
    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
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

    public void deleteById(Long id) {
        repository.deleteById(id);
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
            existing.setCourse(updatedStudent.getCourse());
            existing.setLocation(updatedStudent.getLocation());
            return repository.save(existing);
        } else {
            throw new RuntimeException("Student not found with id: " + id);
        }
    }
}
