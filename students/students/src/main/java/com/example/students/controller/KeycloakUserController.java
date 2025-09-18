package com.example.students.controller;

import com.example.students.service.KeycloakService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/keycloak-users")
public class KeycloakUserController {

    private final KeycloakService keycloakService;

    public KeycloakUserController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    // Show the create user form
    @GetMapping("/new")
    public String showUserForm() {
        return "create-user";  // thymeleaf page
    }

    // Handle form submit
    @PostMapping
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             Model model) {
        try {
            keycloakService.createUser(username, email, password, role);
            model.addAttribute("message", "User created successfully in Keycloak!");
        } catch (Exception e) {
            model.addAttribute("error", "Error creating user: " + e.getMessage());
        }
        return "create-user";
    }
}
