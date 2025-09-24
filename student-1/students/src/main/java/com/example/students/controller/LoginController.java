package com.example.students.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model,
            Authentication authentication,
            HttpServletResponse response) {

        // If already authenticated, redirect away from /login
        if (authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated()) {
            boolean hasAnyPermission = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority() != null && a.getAuthority().startsWith("PERM_"));
            return hasAnyPermission ? "redirect:/admin/dashboard" : "redirect:/user/dashboard";
        }

        // Prevent caching of the login page so back button doesn't show it while authenticated
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (error != null) {
            model.addAttribute("errorMsg", "Invalid login, please try again.");
        }

        if (logout != null) {
            model.addAttribute("logoutMsg", "You have been logged out successfully.");
        }

        return "login"; // → maps to login.html in templates
    }
}
