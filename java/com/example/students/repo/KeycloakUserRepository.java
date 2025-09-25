package com.example.students.repo;

import com.example.students.model.KeycloakUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeycloakUserRepository extends JpaRepository<KeycloakUser, Long> {
    KeycloakUser findByUsername(String username);
    KeycloakUser findByEmail(String email);

    // ✅ New finder
    KeycloakUser findByKeycloakId(String keycloakId);
    java.util.List<KeycloakUser> findAllByRoleIgnoreCase(String role);
}
