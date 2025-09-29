package com.example.students.controller;

import com.example.students.annotation.RequirePermission;
import com.example.students.model.KeycloakUser;
import com.example.students.service.KeycloakService;
import com.example.students.service.KeycloakUserService;
import com.example.students.service.PermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/keycloak-users")
public class KeycloakUserController {

    private final KeycloakService keycloakService;
    private final KeycloakUserService keycloakUserService;
    private final PermissionService permissionService;

    public KeycloakUserController(KeycloakService keycloakService,
                                  KeycloakUserService keycloakUserService,
                                  PermissionService permissionService) {
        this.keycloakService = keycloakService;
        this.keycloakUserService = keycloakUserService;
        this.permissionService = permissionService;
    }

    // ✅ Show list
    @GetMapping
    @RequirePermission(anyOf = {"A", "B", "C", "D"})
    public String listUsers(Model model) {
        List<KeycloakUser> users = keycloakUserService.findAll();
        model.addAttribute("users", users);
        return "keycloak-users";
    }

    // ✅ Show create form
    @GetMapping("/new")
    @RequirePermission("A")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new KeycloakUser());
        // Load all permissions for checkbox list
        model.addAttribute("allPermissions", permissionService.getActivePermissions());
        return "create-user";
    }

    // ✅ Handle form submit (create user)
    @PostMapping
    @RequirePermission("A")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             @RequestParam(name = "codes", required = false) String[] codes,
                             @RequestParam(name = "grantedBy", required = false) String grantedBy,
                             Model model) {
        try {
            // Create user and assign Keycloak groups according to selected permission codes
            String kcId = (codes != null && codes.length > 0)
                    ? keycloakService.createUser(username, email, password, role, java.util.Arrays.asList(codes))
                    : keycloakService.createUser(username, email, password, role);

            KeycloakUser kcUser = new KeycloakUser();
            kcUser.setUsername(username);
            kcUser.setEmail(email);
            kcUser.setRole(role);
            kcUser.setKeycloakId(kcId);
            keycloakUserService.save(kcUser);

            // Assign selected permissions (if any)
            if (codes != null && codes.length > 0) {
                java.util.List<String> codesList = java.util.Arrays.asList(codes);
                String issuer = (grantedBy == null || grantedBy.isBlank()) ? username : grantedBy;
                permissionService.assignPermissionsToUser(username, codesList, issuer);
            }

            return "redirect:/admin/keycloak-users";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating user: " + e.getMessage());
            model.addAttribute("allPermissions", permissionService.getActivePermissions());
            return "create-user";
        }
    }

    // ✅ Update user with role-based restriction
    @PostMapping("/update/{id}")
    @RequirePermission("C")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String email,
                             @RequestParam String role,
                             @RequestParam(required = false) String password,
                             Authentication authentication) {

        KeycloakUser user = keycloakUserService.findById(id);

        // If password change is requested, require D permission
        if (password != null && !password.isBlank()) {
            String currentUsername = extractUsernameFromAuth();
            if (!permissionService.hasPermission(currentUsername, "D")) {
                throw new AccessDeniedException("Insufficient permissions to change password");
            }
        }

        // Update in Keycloak
        keycloakService.updateUser(user.getKeycloakId(), email, role, password);

        // Update in local DB
        user.setEmail(email);
        user.setRole(role);
        keycloakUserService.save(user);

        return "redirect:/admin/keycloak-users";
    }

    // ✅ Delete user (admin & manager only)
    @PostMapping("/delete/{id}")
    @RequirePermission("B")
    public String deleteUser(@PathVariable Long id) {
        KeycloakUser user = keycloakUserService.findById(id);

        permissionService.removePermissionsForUser(user);
        keycloakService.deleteUser(user.getKeycloakId());
        keycloakUserService.deleteById(id);

        return "redirect:/admin/keycloak-users";
    }

    private String extractUsernameFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof OidcUser oidc) {
            return oidc.getPreferredUsername();
        }
        return auth.getName();
    }
}
