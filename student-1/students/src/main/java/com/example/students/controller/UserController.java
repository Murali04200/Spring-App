package com.example.students.controller;

import com.example.students.service.KeycloakService;
import org.springframework.security.core.Authentication;
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
                                 RedirectAttributes redirectAttributes) {

        String username = authentication.getName(); // logged-in user's username

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirm password do not match.");
            return "redirect:/user/dashboard";
        }

        try {
            // Call KeycloakService to update self password
            keycloakService.changeOwnPassword(username, oldPassword, newPassword);

            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update password: " + e.getMessage());
        }

        return "redirect:/user/dashboard";
    }
}
