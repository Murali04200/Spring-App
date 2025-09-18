package com.example.students.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOidcUserService extends OidcUserService {

    // ✅ List of allowed admin emails
    private final Set<String> adminEmails = Set.of(
            "admin@example.com",
            "siva@example.com",
            "karthi@gmail.com"
    );

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        Set<String> mappedAuthorities = new HashSet<>();

        if (adminEmails.contains(oidcUser.getEmail())) {
            mappedAuthorities.add("ROLE_ADMIN");
        } else {
            mappedAuthorities.add("ROLE_USER");
        }

        return new DefaultOidcUser(
                mappedAuthorities.stream()
                        .map(role -> (GrantedAuthority) () -> role)
                        .collect(Collectors.toSet()),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }
}
