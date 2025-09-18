package com.example.students.service;

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
    private final String clientSecret = "MLwTVdk3VyT7t5FwHyzbr0Ak9gKh0RM0";

    public KeycloakService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(keycloakUrl).build();
    }

    private String getAdminToken() {
        Map tokenResponse = webClient.post()
                .uri("/realms/myrealm/protocol/openid-connect/token")
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

    public void createUser(String username, String email, String password, String role) {
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

        if (userId == null) {
            throw new RuntimeException("User creation failed, userId is null");
        }

        // 2. Set password
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
        String assignedRole = (role == null || role.isBlank()) ? "USER" : role; // ✅ Default to USER if null/empty

        Map roleObj = webClient.get()
                .uri("/admin/realms/" + realm + "/roles/{roleName}", assignedRole)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (roleObj == null) {
            throw new RuntimeException("Role not found in Keycloak: " + assignedRole);
        }

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
