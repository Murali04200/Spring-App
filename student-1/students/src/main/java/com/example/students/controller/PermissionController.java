package com.example.students.controller;

import com.example.students.annotation.RequirePermission;
import com.example.students.model.Permission;
import com.example.students.service.PermissionService;
import com.example.students.service.KeycloakService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/admin/permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final KeycloakService keycloakService;

    public PermissionController(PermissionService permissionService,
                                KeycloakService keycloakService) {
        this.permissionService = permissionService;
        this.keycloakService = keycloakService;
    }

    // List all permissions (requires E: VIEW_REPORTS)
    @GetMapping
    @RequirePermission("E")
    public String listPermissions(Model model) {
        List<Permission> permissions = permissionService.getAllPermissions();
        model.addAttribute("permissions", permissions);
        return "permissions"; // thymeleaf view
    }

    // View a user's permissions (G only)
    @GetMapping("/user/{username}")
    @RequirePermission("G")
    public String viewUserPermissions(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        model.addAttribute("userPermissions", permissionService.getUserPermissionDetails(username));
        model.addAttribute("allPermissions", permissionService.getActivePermissions());
        return "user-permissions"; // thymeleaf view
    }

    // Render create-permission form (F)
    @GetMapping("/new")
    @RequirePermission("F")
    public String newPermissionForm(Model model) {
        return "create-permission";
    }

    // Handle create-permission submit (F)
    @PostMapping
    @RequirePermission("F")
    public String createPermission(@RequestParam String code,
                                   @RequestParam String name,
                                   @RequestParam String description,
                                   @RequestParam(name = "active", defaultValue = "true") boolean active,
                                   Model model) {
        String normalized = code == null ? null : code.trim().toUpperCase();
        if (normalized == null || !normalized.matches("^[A-Z]$")) {
            model.addAttribute("error", "Code must be a single letter A-Z");
            return "create-permission";
        }
        // Create in DB
        Permission p = permissionService.getPermissionByCode(normalized).orElse(null);
        if (p == null) {
            p = permissionService.createPermission(normalized, name, description);
        } else {
            permissionService.updatePermission(p.getId(), name, description, active);
        }
        // Ensure Keycloak group exists with same code
        keycloakService.ensureGroupExists(normalized);
        return "redirect:/admin/permissions";
    }

    // Assign permissions to a user (G only)
    @PostMapping("/assign")
    @RequirePermission("G")
    public String assignPermissions(@RequestParam String username,
                                    @RequestParam(name = "codes", required = false) String[] codes,
                                    @RequestParam(name = "grantedBy", required = false) String grantedBy,
                                    Authentication authentication) {
        // If nothing selected, just redirect back without changes
        if (codes == null) codes = new String[0];
        String issuer = grantedBy;
        if (issuer == null || issuer.isBlank()) {
            issuer = (authentication != null ? authentication.getName() : "system");
        }
        permissionService.assignPermissionsToUser(username, Arrays.asList(codes), issuer);

        // Sync Keycloak groups (A/B/C/D/E) to match selected permissions so token authorities align
        Set<String> desired = new HashSet<>();
        for (String c : codes) { if (c != null && !c.isBlank()) desired.add(c.trim().toUpperCase()); }
        try {
            keycloakService.syncUserPermissionGroups(username, desired);
        } catch (Exception ex) {
            // Do not fail UX on sync issues; just log to console for now
            System.out.println("Failed to sync Keycloak groups for user " + username + ": " + ex.getMessage());
        }
        return "redirect:/admin/permissions/user/" + username;
    }
}
