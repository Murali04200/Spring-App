package com.example.students.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        // Extract Keycloak realm roles
        Map<String, Object> realmAccess = oidcUser.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            for (Object role : roles) {
                String roleName = role.toString().toLowerCase(); // normalize
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }

        // Debug log
        System.out.println("👉 Roles from Keycloak: " + mappedAuthorities);

        return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
