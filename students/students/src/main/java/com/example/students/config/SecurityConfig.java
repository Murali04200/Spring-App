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
    private static final String REDIRECT_URI = "http://localhost:8081/";

    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(CustomOidcUserService customOidcUserService) {
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers("/students/new").hasRole("admin") // lowercase
                        .requestMatchers("/students/update/**").hasAnyRole("admin","manager","jrmanager","clerk") // added
                        .requestMatchers("/students/delete/**").hasAnyRole("admin","manager") //  added
                        .requestMatchers("/students").hasAnyRole("admin", "user")
                        .requestMatchers("/keycloak-users/**").hasRole("admin")
                        .requestMatchers("/admin/**").hasAnyRole("admin","manager","jrmanager")
                        .requestMatchers("/dashboard").hasRole("admin")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .successHandler(successHandler())
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
