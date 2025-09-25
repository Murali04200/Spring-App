package com.example.students.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String KEYCLOAK_ISSUER = "http://localhost:8080/realms/myrealm";
    private static final String CLIENT_ID = "students-client";

    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(CustomOidcUserService customOidcUserService) {
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/login").permitAll()

                        // Public endpoints
                        .requestMatchers("/students").authenticated() // All authenticated users can view
                        .requestMatchers("/user/dashboard").authenticated()
                        
                        // Admin endpoints - will be protected by @RequirePermission annotations
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers("/students/new", "/students/update/**", "/students/delete/**").authenticated()
                        .requestMatchers("/keycloak-users/**").authenticated()
                        .requestMatchers("/admin/permissions/**").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .successHandler(successHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // POST /logout with CSRF
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String logoutUrl = KEYCLOAK_ISSUER + "/protocol/openid-connect/logout"
                                    + "?client_id=" + CLIENT_ID
                                    + "&post_logout_redirect_uri=http://localhost:8081/login?logout";
                            response.sendRedirect(logoutUrl);
                        })
                        .permitAll()
                )
                // ✅ FIX: Keep session alive until explicit logout
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            var authorities = authentication.getAuthorities();

            String redirectUrl = "/user/dashboard"; // default

            // Permission-based redirect: if user has any PERM_* authority, consider them admin-eligible
            boolean hasAnyPermission = authorities.stream()
                    .anyMatch(a -> a.getAuthority() != null && a.getAuthority().startsWith("PERM_"));
            if (hasAnyPermission) {
                redirectUrl = "/admin/dashboard";
            }

            response.sendRedirect(redirectUrl);
        };
    }
}
