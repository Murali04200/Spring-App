package com.example.students.controller;

import com.example.students.service.KeycloakService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {

    private final KeycloakService keycloakService;

    public UserController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    // ✅ Password change endpoint for user & clerk
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes,
                                 HttpServletRequest request) {

        String username = authentication.getName(); // logged-in user's username

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirm password do not match.");
            return "redirect:" + resolveReturnUrl(authentication, request);
        }

        try {
            // Call KeycloakService to update self password
            keycloakService.changeOwnPassword(username, oldPassword, newPassword);

            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update password: " + e.getMessage());
        }

        return "redirect:" + resolveReturnUrl(authentication, request);
    }

    private String resolveReturnUrl(Authentication authentication, HttpServletRequest request) {
        // Prefer Referer to stay on the same page (e.g., admin dashboard)
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            // Only allow redirecting within our app
            try {
                java.net.URI uri = java.net.URI.create(referer);
                String path = uri.getPath();
                if (path != null && !path.isBlank()) {
                    return path;
                }
            } catch (Exception ignored) {}
        }
        // Fallback: if user has any PERM_*, treat as admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a != null && a.startsWith("PERM_"));
        return isAdmin ? "/admin/dashboard" : "/user/dashboard";
    }
}
