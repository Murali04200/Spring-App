package com.example.students.config;

import com.example.students.service.PermissionService;
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

    private final PermissionService permissionService;

    public CustomOidcUserService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        // ✅ Extract Keycloak realm roles
        Map<String, Object> realmAccess = oidcUser.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            for (Object role : roles) {
                String roleName = role.toString().toLowerCase(); // normalize
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }

        // ✅ Extract Keycloak groups and map to PERM_<CODE>
        List<String> groups = oidcUser.getClaimAsStringList("groups");
        if (groups != null) {
            for (String group : groups) {
                String code = normalizeGroupToPermissionCode(group);
                if (code != null) {
                    // Auto-create DB permission if missing so it appears in app dropdowns immediately
                    permissionService.getPermissionByCode(code).orElseGet(() ->
                            permissionService.createPermission(
                                    code,
                                    "CUSTOM_" + code,
                                    "Auto-created from Keycloak group " + code
                            )
                    );
                    mappedAuthorities.add(new SimpleGrantedAuthority("PERM_" + code));
                }
            }
        }

        // ✅ Extract username or full name for display
        String username = oidcUser.getPreferredUsername(); // usually the Keycloak username
        if (username == null || username.isBlank()) {
            username = oidcUser.getFullName(); // fallback to full name
        }
        if (username == null || username.isBlank()) {
            username = oidcUser.getEmail(); // fallback to email
        }

        // Debug log
        System.out.println("👉 Roles from Keycloak: " + mappedAuthorities);
        System.out.println("👉 Groups from Keycloak: " + groups);
        System.out.println("👉 Logged in user: " + username);

        // ✅ Map "preferred_username" so Thymeleaf can show it
        return new DefaultOidcUser(
                mappedAuthorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "preferred_username"
        );
    }

    private String normalizeGroupToPermissionCode(String group) {
        if (group == null || group.isBlank()) return null;
        // Typical group paths: "/A", "/permissions/A" or simple "A"
        String g = group.trim();
        int idx = g.lastIndexOf('/');
        if (idx >= 0 && idx < g.length() - 1) {
            g = g.substring(idx + 1);
        }
        g = g.toUpperCase();
        // Accept A,B,C,D,E directly or prefixed forms like PERM_A
        if (g.matches("[A-Z]+") && (g.length() == 1 || g.equals("A") || g.equals("B") || g.equals("C") || g.equals("D") || g.equals("E"))) {
            return g.length() == 1 ? g : null; // only single-letter codes
        }
        if (g.startsWith("PERM_") && g.length() == 6) {
            return String.valueOf(g.charAt(5));
        }
        return null;
    }
}
