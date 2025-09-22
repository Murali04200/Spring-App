package com.example.students.controller;

import com.example.students.model.KeycloakUser;
import com.example.students.service.KeycloakService;
import com.example.students.service.KeycloakUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/keycloak-users")
public class KeycloakUserController {

    private final KeycloakService keycloakService;
    private final KeycloakUserService keycloakUserService;

    public KeycloakUserController(KeycloakService keycloakService,
                                  KeycloakUserService keycloakUserService) {
        this.keycloakService = keycloakService;
        this.keycloakUserService = keycloakUserService;
    }

    // ✅ Show list
    @GetMapping
    public String listUsers(Model model) {
        List<KeycloakUser> users = keycloakUserService.findAll();
        model.addAttribute("users", users);
        return "keycloak-users";
    }

    // ✅ Show create form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new KeycloakUser());
        return "create-user";
    }

    // ✅ Handle form submit (create user)
    @PostMapping
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             Model model) {
        try {
            String kcId = keycloakService.createUser(username, email, password, role);

            KeycloakUser kcUser = new KeycloakUser();
            kcUser.setUsername(username);
            kcUser.setEmail(email);
            kcUser.setRole(role);
            kcUser.setKeycloakId(kcId);
            keycloakUserService.save(kcUser);

            return "redirect:/admin/keycloak-users";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating user: " + e.getMessage());
            return "create-user";
        }
    }

    // ✅ Update user (email, role, password)
    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String email,
                             @RequestParam String role,
                             @RequestParam(required = false) String password) {
        KeycloakUser user = keycloakUserService.findById(id);

        // Update in Keycloak
        keycloakService.updateUser(user.getKeycloakId(), email, role, password);

        // Update in local DB
        user.setEmail(email);
        user.setRole(role);

        // ❌ Do NOT store password in DB (only update in Keycloak)
        keycloakUserService.save(user);

        return "redirect:/admin/keycloak-users";
    }


    // ✅ Delete user
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        KeycloakUser user = keycloakUserService.findById(id);

        keycloakService.deleteUser(user.getKeycloakId());
        keycloakUserService.deleteById(id);

        return "redirect:/admin/keycloak-users";
    }
}
