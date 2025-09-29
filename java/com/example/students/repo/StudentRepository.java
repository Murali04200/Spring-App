package com.example.students.repo;

import com.example.students.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("select s.className, count(s) from Student s where s.className is not null group by s.className order by s.className")
    List<Object[]> countStudentsByClass();

    @Query("select avg(s.totalMarks) from Student s where s.totalMarks is not null")
    Double averageTotalMarks();
}
