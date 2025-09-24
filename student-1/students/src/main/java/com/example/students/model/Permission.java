package com.example.students.model;

import jakarta.persistence.*;

@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code; // A, B, C, D, E, etc.

    @Column(nullable = false, length = 100)
    private String name; // Short name like "CREATE_USER"

    @Column(nullable = false, length = 255)
    private String description; // Full description of what this permission allows

    @Column(nullable = false)
    private Boolean active = true; // To enable/disable permissions

    // Constructors
    public Permission() {}

    public Permission(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", active=" + active +
                '}';
    }
}
