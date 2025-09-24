package com.example.students.service;

import com.example.students.model.KeycloakUser;
import com.example.students.repo.KeycloakUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakUserService {

    private final KeycloakUserRepository repository;

    public KeycloakUserService(KeycloakUserRepository repository) {
        this.repository = repository;
    }

    public KeycloakUser save(KeycloakUser user) {
        return repository.save(user);
    }

    public List<KeycloakUser> findAll() {
        return repository.findAll();
    }

    public KeycloakUser findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public KeycloakUser findByKeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId);
    }
}
