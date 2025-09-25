package com.example.students.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subject", uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code; // e.g., MATH, ENG, SCI

    @Column(nullable = false, length = 100)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
