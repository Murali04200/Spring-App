package com.example.students.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
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

    // Ensure a Keycloak group with given name exists; create if missing
    public void ensureGroupExists(String code) {
        if (code == null || code.isBlank()) return;
        String normalized = code.trim().toUpperCase();
        String existing = findGroupIdByName(normalized);
        if (existing != null) return;
        String token = getAdminToken();
        java.util.Map<String, Object> payload = java.util.Map.of("name", normalized);
        webClient.post()
                .uri("/admin/realms/" + realm + "/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    // Fetch admin token
    private String getAdminToken() {
        Map<String, Object> tokenResponse = webClient.post()
                .uri("/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Failed to fetch admin access token from Keycloak");
        }

        return tokenResponse.get("access_token").toString();
    }

    // ===========================
    // Admin-level Operations
    // ===========================

    // Create user and (legacy) assign role
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(passwordPayload)
                .retrieve()
                .toBodilessEntity()
                .block();
        return userId;
    }

    // Create user and assign permission groups (A/B/C/D/E...) in Keycloak
    public String createUser(String username, String email, String password, String role, java.util.List<String> permissionCodes) {
        String userId = createUser(username, email, password, role);
        if (permissionCodes != null && !permissionCodes.isEmpty()) {
            for (String code : permissionCodes) {
                try {
                    String groupId = findGroupIdByName(code);
                    if (groupId != null) {
                        addUserToGroup(userId, groupId);
                    } else {
                        System.out.println("Keycloak group not found for code: " + code);
                    }
                } catch (Exception ex) {
                    System.out.println("Failed to add user to group for code " + code + ": " + ex.getMessage());
                }
            }
        }
        return userId;
    }

    private String findGroupIdByName(String code) {
        String token = getAdminToken();
        // search by name; Keycloak returns list
        List<Map<String, Object>> groups = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/" + realm + "/groups")
                        .queryParam("search", code)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();

        if (groups == null || groups.isEmpty()) return null;

        // try exact match on name (case-insensitive)
        for (Map<String, Object> g : groups) {
            Object name = g.get("name");
            if (name != null && name.toString().equalsIgnoreCase(code)) {
                Object id = g.get("id");
                return id != null ? id.toString() : null;
            }
        }

        // fallback to first
        Object id = groups.get(0).get("id");
        return id != null ? id.toString() : null;
    }

    private void addUserToGroup(String userId, String groupId) {
        String token = getAdminToken();
        webClient.put()
                .uri("/admin/realms/" + realm + "/users/{userId}/groups/{groupId}", userId, groupId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private void removeUserFromGroup(String userId, String groupId) {
        String token = getAdminToken();
        webClient.delete()
                .uri("/admin/realms/" + realm + "/users/{userId}/groups/{groupId}", userId, groupId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String findUserIdByUsername(String username) {
        String token = getAdminToken();
        List<Map<String, Object>> users = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/" + realm + "/users")
                        .queryParam("username", username)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();
        if (users == null || users.isEmpty()) return null;
        Object id = users.get(0).get("id");
        return id != null ? id.toString() : null;
    }

    private List<Map<String, Object>> listUserGroups(String userId) {
        String token = getAdminToken();
        return webClient.get()
                .uri("/admin/realms/" + realm + "/users/{id}/groups", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();
    }

    // Sync Keycloak groups (A/B/C/D/E/...) to match desired codes for a username
    public void syncUserPermissionGroups(String username, java.util.Set<String> desiredCodes) {
        if (desiredCodes == null) desiredCodes = java.util.Collections.emptySet();
        String userId = findUserIdByUsername(username);
        if (userId == null) throw new RuntimeException("Keycloak user not found by username: " + username);

        // Current groups
        List<Map<String, Object>> current = listUserGroups(userId);
        java.util.Set<String> currentCodes = new java.util.HashSet<>();
        if (current != null) {
            for (Map<String, Object> g : current) {
                Object name = g.get("name");
                if (name != null) {
                    String n = name.toString().trim().toUpperCase();
                    if (n.length() == 1 && n.matches("[A-Z]")) currentCodes.add(n);
                }
            }
        }

        // Remove groups not desired
        for (String code : currentCodes) {
            if (!desiredCodes.contains(code)) {
                String gid = findGroupIdByName(code);
                if (gid != null) removeUserFromGroup(userId, gid);
            }
        }

        // Add missing desired groups
        for (String code : desiredCodes) {
            if (!currentCodes.contains(code)) {
                String gid = findGroupIdByName(code);
                if (gid != null) addUserToGroup(userId, gid);
            }
        }
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

        // 3. Skip role changes to keep system strictly permission (group) based
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

    // Self-service: change own password using ROPC verify
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
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Old password is incorrect or Keycloak login failed");
        }

        // 2. Fetch userId
        String userId = webClient.get()
                .uri("/admin/realms/" + realm + "/users?username={username}", username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(m -> m.get("id"))
                .cast(Object.class)
                .map(Object::toString)
                .blockFirst();

        // 3. Reset password in Keycloak
        Map<String, Object> passwordPayload2 = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        webClient.put()
                .uri("/admin/realms/" + realm + "/users/{id}/reset-password", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(passwordPayload2)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
