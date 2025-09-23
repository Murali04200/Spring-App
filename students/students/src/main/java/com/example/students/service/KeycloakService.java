package com.example.students.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    private final WebClient webClient;

    private final String keycloakUrl = "http://localhost:8080";
    private final String realm = "myrealm";

    private final String clientId = "spring-admin-client";
    private final String clientSecret = "QWVvIi3KKH61dntpquKkOYRO59f2bbuf";

    public KeycloakService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(keycloakUrl).build();
    }

    // Fetch admin token
    private String getAdminToken() {
        Map tokenResponse = webClient.post()
                .uri("/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Failed to fetch admin access token from Keycloak");
        }

        return tokenResponse.get("access_token").toString();
    }

    // ===========================
    // Admin-level Operations
    // ===========================

    // Create user and assign role
    public String createUser(String username, String email, String password, String role) {
        String token = getAdminToken();

        // 1. Create user
        Map<String, Object> user = Map.of(
                "username", username,
                "email", email,
                "enabled", true,
                "emailVerified", true
        );

        String userId = webClient.post()
                .uri("/admin/realms/" + realm + "/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        String location = response.headers().asHttpHeaders().getFirst("Location");
                        if (location == null) {
                            return Mono.error(new RuntimeException("No Location header returned when creating user"));
                        }
                        return Mono.just(location.substring(location.lastIndexOf("/") + 1));
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new RuntimeException("User creation failed: " + err)));
                    }
                })
                .block();

        if (userId == null) throw new RuntimeException("User creation failed, userId is null");

        // 2. Set initial password
        Map<String, Object> passwordPayload = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        webClient.put()
                .uri("/admin/realms/" + realm + "/users/{id}/reset-password", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(passwordPayload)
                .retrieve()
                .toBodilessEntity()
                .block();

        // 3. Assign role
        String assignedRole = (role == null || role.isBlank()) ? "USER" : role;

        Map roleObj = webClient.get()
                .uri("/admin/realms/" + realm + "/roles/{roleName}", assignedRole)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (roleObj == null) throw new RuntimeException("Role not found in Keycloak: " + assignedRole);

        webClient.post()
                .uri("/admin/realms/" + realm + "/users/{id}/role-mappings/realm", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(roleObj))
                .retrieve()
                .toBodilessEntity()
                .block();

        return userId;
    }

    // Update user (email, role, password)
    public void updateUser(String userId, String email, String role, String password) {
        String token = getAdminToken();

        // 1. Update basic user info
        Map<String, Object> updatedUser = Map.of(
                "email", email,
                "enabled", true,
                "emailVerified", true
        );

        webClient.put()
                .uri("/admin/realms/" + realm + "/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedUser)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) return Mono.empty();
                    else return response.bodyToMono(String.class)
                            .flatMap(err -> Mono.error(new RuntimeException(
                                    "Update failed: " + response.statusCode() + " → " + err
                            )));
                })
                .block();

        // 2. Update password if provided
        if (password != null && !password.isBlank()) {
            Map<String, Object> passwordPayload = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            );

            webClient.put()
                    .uri("/admin/realms/" + realm + "/users/{id}/reset-password", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(passwordPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        }

        // 3. Update role
        String assignedRole = (role == null || role.isBlank()) ? "USER" : role;

        Map roleObj = webClient.get()
                .uri("/admin/realms/" + realm + "/roles/{roleName}", assignedRole)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (roleObj != null) {
            // Remove existing roles
            List<Map> existingRoles = webClient.get()
                    .uri("/admin/realms/" + realm + "/users/{id}/role-mappings/realm", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();

            if (existingRoles != null && !existingRoles.isEmpty()) {
                webClient.method(HttpMethod.DELETE)
                        .uri("/admin/realms/" + realm + "/users/{id}/role-mappings/realm", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(existingRoles)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            }

            // Assign new role
            webClient.post()
                    .uri("/admin/realms/" + realm + "/users/{id}/role-mappings/realm", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(roleObj))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        }
    }

    // Delete user
    public void deleteUser(String userId) {
        String token = getAdminToken();

        webClient.delete()
                .uri("/admin/realms/" + realm + "/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    // ===========================
    // Self-service Operations (User & Clerk)
    // ===========================

    public void changeOwnPassword(String username, String oldPassword, String newPassword) {
        // 1. Verify old password via ROPC
        Map<String, Object> tokenResponse = webClient.post()
                .uri("/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("username", username)
                        .with("password", oldPassword))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Old password is incorrect or Keycloak login failed");
        }

        // 2. Fetch userId
        String userId = webClient.get()
                .uri("/admin/realms/" + realm + "/users?username={username}", username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
                .retrieve()
                .bodyToFlux(Map.class)
                .blockFirst()
                .get("id").toString();

        // 3. Reset password in Keycloak
        Map<String, Object> passwordPayload = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        webClient.put()
                .uri("/admin/realms/" + realm + "/users/{id}/reset-password", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(passwordPayload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
