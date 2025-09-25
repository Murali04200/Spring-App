package com.example.students.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student")
public class Student {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String email;

	// New fields for class and total marks
	@Column(name = "class_name")
	private String className; // e.g., Class 10A

	@Column(name = "total_marks")
	private Integer totalMarks; // denormalized aggregate for quick view

	public String getClassName() { return className; }
	public void setClassName(String className) { this.className = className; }

	public Integer getTotalMarks() { return totalMarks; }
	public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
}
