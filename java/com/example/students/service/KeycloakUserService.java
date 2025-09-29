package com.example.students.service;

import com.example.students.model.KeycloakUser;
import com.example.students.repo.KeycloakUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakUserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);

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
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException ex) {
            log.debug("KeycloakUser with id {} already removed: {}", id, ex.getMessage());
        }
    }

    public KeycloakUser findByKeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId);
    }
}
