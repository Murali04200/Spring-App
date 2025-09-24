package com.example.students.aspect;

import com.example.students.annotation.RequirePermission;
import com.example.students.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        String username = getCurrentUsername(authentication);
        if (username == null) {
            throw new AccessDeniedException("Unable to determine username");
        }

        // Check permissions
        boolean hasAccess = checkUserPermissions(username, requirePermission) ||
                checkAuthorityPermissions(authentication, requirePermission);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Insufficient permissions");
        }

        return joinPoint.proceed();
    }

    private String getCurrentUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            return oidcUser.getPreferredUsername();
        }
        return authentication.getName();
    }

    private boolean checkUserPermissions(String username, RequirePermission requirePermission) {
        String[] requiredPermissions = requirePermission.value();
        String[] anyOfPermissions = requirePermission.anyOf();
        boolean requireAny = requirePermission.requireAny();

        // Check anyOf permissions first
        if (anyOfPermissions.length > 0) {
            return permissionService.hasAnyPermission(username, anyOfPermissions);
        }

        // Check required permissions
        if (requiredPermissions.length > 0) {
            if (requireAny) {
                return permissionService.hasAnyPermission(username, requiredPermissions);
            } else {
                return permissionService.hasAllPermissions(username, requiredPermissions);
            }
        }

        // No permissions specified - allow access
        return true;
    }

    private boolean checkAuthorityPermissions(Authentication authentication, RequirePermission requirePermission) {
        String[] requiredPermissions = requirePermission.value();
        String[] anyOfPermissions = requirePermission.anyOf();
        boolean requireAny = requirePermission.requireAny();

        // Build set of PERM_ codes from authorities
        java.util.Set<String> perms = new java.util.HashSet<>();
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String a = auth.getAuthority();
            if (a != null && a.startsWith("PERM_") && a.length() == 6) {
                perms.add(String.valueOf(a.charAt(5)));
            }
        }

        if (anyOfPermissions.length > 0) {
            for (String code : anyOfPermissions) {
                if (perms.contains(code)) return true;
            }
            return false;
        }

        if (requiredPermissions.length > 0) {
            if (requireAny) {
                for (String code : requiredPermissions) {
                    if (perms.contains(code)) return true;
                }
                return false;
            } else {
                for (String code : requiredPermissions) {
                    if (!perms.contains(code)) return false;
                }
                return true;
            }
        }

        return true;
    }
}
