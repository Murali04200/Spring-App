package com.example.students.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_permission", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_id"}))
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private KeycloakUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "granted_by", nullable = false)
    private String grantedBy; // Username of admin who granted this permission

    @Column(name = "granted_at", nullable = false)
    private java.time.LocalDateTime grantedAt;

    @Column(nullable = false)
    private Boolean active = true; // To temporarily disable specific user permissions

    // Constructors
    public UserPermission() {}

    public UserPermission(KeycloakUser user, Permission permission, String grantedBy) {
        this.user = user;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = java.time.LocalDateTime.now();
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KeycloakUser getUser() {
        return user;
    }

    public void setUser(KeycloakUser user) {
        this.user = user;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public java.time.LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(java.time.LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "UserPermission{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : null) +
                ", permission=" + (permission != null ? permission.getCode() : null) +
                ", grantedBy='" + grantedBy + '\'' +
                ", grantedAt=" + grantedAt +
                ", active=" + active +
                '}';
    }
}
