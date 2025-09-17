package com.example.students.config;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final String adminEmails = "admin@example.com";

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
                mappedAuthorities.stream().map(a -> (org.springframework.security.core.GrantedAuthority) () -> a).toList(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }
}
