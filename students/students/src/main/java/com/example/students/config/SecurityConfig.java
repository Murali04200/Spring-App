package com.example.students.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String KEYCLOAK_ISSUER = "http://localhost:8080/realms/myrealm";
    private static final String CLIENT_ID = "students-client";
    private static final String REDIRECT_URI = "http://localhost:8081/oauth2/authorization/keycloak";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/students/new").hasRole("ADMIN")
                        .requestMatchers("/students").hasAnyRole("ADMIN", "USER")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler()) // redirect based on role
                )
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String logoutUrl = KEYCLOAK_ISSUER + "/protocol/openid-connect/logout"
                                    + "?client_id=" + CLIENT_ID
                                    + "&post_logout_redirect_uri=" + REDIRECT_URI;
                            response.sendRedirect(logoutUrl);
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            var authorities = authentication.getAuthorities();
            String redirectUrl = "/students";

            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                redirectUrl = "/students/new";
            }

            response.sendRedirect(redirectUrl);
        };
    }
}
