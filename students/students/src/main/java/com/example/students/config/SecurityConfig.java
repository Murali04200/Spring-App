package com.example.students.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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

                        // Students endpoints
                        .requestMatchers("/students/new").hasAnyRole("admin","manager","jrmanager")
                        .requestMatchers("/students/update/**").hasAnyRole("admin","manager","jrmanager","clerk")
                        .requestMatchers("/students/delete/**").hasAnyRole("admin","manager")
                        .requestMatchers("/students").hasAnyRole("admin", "manager","jrmanager","clerk","user")

                        // Keycloak management endpoints
                        .requestMatchers("/keycloak-users/new").hasAnyRole("admin","manager")
                        .requestMatchers("/keycloak-users/update/**").hasAnyRole("admin","manager","jrmanager")
                        .requestMatchers("/keycloak-users/delete/**").hasAnyRole("admin","manager")

                        .requestMatchers("/admin/**").hasAnyRole("admin","manager","jrmanager")
                        .requestMatchers("/dashboard").hasRole("admin")

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .successHandler(successHandler())
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
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

            String redirectUrl = "/user/dashboard"; // default for user

            if (authorities.stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_admin") ||
                            a.getAuthority().equals("ROLE_manager") ||
                            a.getAuthority().equals("ROLE_jrmanager"))) {
                redirectUrl = "/admin/dashboard"; // admin, manager, jrmanager → dashboard
            }

            response.sendRedirect(redirectUrl);
        };
    }
}
