package com.example.students.service;

import com.example.students.model.Permission;
import com.example.students.model.UserPermission;
import com.example.students.model.KeycloakUser;
import com.example.students.repo.PermissionRepository;
import com.example.students.repo.UserPermissionRepository;
import com.example.students.repo.KeycloakUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final KeycloakUserRepository keycloakUserRepository;

    public PermissionService(PermissionRepository permissionRepository,
                           UserPermissionRepository userPermissionRepository,
                           KeycloakUserRepository keycloakUserRepository) {
        this.permissionRepository = permissionRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.keycloakUserRepository = keycloakUserRepository;
    }

    // Initialize default permissions
    public void initializeDefaultPermissions() {
        createPermissionIfNotExists("A", "CREATE_USER", "Create user in Keycloak and student details");
        createPermissionIfNotExists("B", "DELETE_USER", "Delete user in Keycloak and students in database");
        createPermissionIfNotExists("C", "UPDATE_USER", "Update Keycloak and student details");
        createPermissionIfNotExists("D", "CHANGE_PASSWORD", "Change password in Keycloak");
        createPermissionIfNotExists("E", "VIEW_REPORTS", "View system reports and analytics");
    }

    private void createPermissionIfNotExists(String code, String name, String description) {
        if (!permissionRepository.existsByCode(code)) {
            Permission permission = new Permission(code, name, description);
            permissionRepository.save(permission);
        }
    }

    // Permission CRUD operations
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<Permission> getActivePermissions() {
        return permissionRepository.findByActiveTrue();
    }

    public Optional<Permission> getPermissionByCode(String code) {
        return permissionRepository.findByCode(code);
    }

    public Permission createPermission(String code, String name, String description) {
        if (permissionRepository.existsByCode(code)) {
            throw new RuntimeException("Permission with code " + code + " already exists");
        }
        Permission permission = new Permission(code, name, description);
        permission.setActive(true);
        return permissionRepository.save(permission);
    }

    public Permission updatePermission(Long id, String name, String description, Boolean active) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        permission.setName(name);
        permission.setDescription(description);
        permission.setActive(active);
        return permissionRepository.save(permission);
    }

    // User Permission operations
    public void assignPermissionsToUser(String username, List<String> permissionCodes, String grantedBy) {
        KeycloakUser user = keycloakUserRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }

        // Normalize and de-duplicate incoming codes
        Set<String> desired = new HashSet<>();
        if (permissionCodes != null) {
            for (String c : permissionCodes) {
                if (c != null && !c.isBlank()) desired.add(c.trim().toUpperCase());
            }
        }

        // Fetch current assignments
        List<UserPermission> current = userPermissionRepository.findByUser(user);

        // Remove assignments that are no longer desired
        for (UserPermission up : current) {
            String code = up.getPermission().getCode();
            if (!desired.contains(code)) {
                userPermissionRepository.delete(up);
            }
        }

        // Add or reactivate desired assignments
        for (String code : desired) {
            Permission permission = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Permission not found: " + code));

            Optional<UserPermission> existing = userPermissionRepository.findByUserAndPermission(user, permission);
            if (existing.isPresent()) {
                UserPermission up = existing.get();
                up.setActive(true);
                up.setGrantedBy(grantedBy);
                up.setGrantedAt(java.time.LocalDateTime.now());
                userPermissionRepository.save(up);
            } else {
                UserPermission userPermission = new UserPermission(user, permission, grantedBy);
                userPermissionRepository.save(userPermission);
            }
        }
    }

    public void addPermissionToUser(String username, String permissionCode, String grantedBy) {
        KeycloakUser user = keycloakUserRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        
        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionCode));

        // Check if user already has this permission
        Optional<UserPermission> existing = userPermissionRepository.findByUserAndPermission(user, permission);
        if (existing.isPresent()) {
            // Reactivate if it was deactivated
            UserPermission userPermission = existing.get();
            userPermission.setActive(true);
            userPermissionRepository.save(userPermission);
        } else {
            // Create new permission assignment
            UserPermission userPermission = new UserPermission(user, permission, grantedBy);
            userPermissionRepository.save(userPermission);
        }
    }

    public void removePermissionFromUser(String username, String permissionCode) {
        KeycloakUser user = keycloakUserRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        
        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionCode));

        userPermissionRepository.deleteByUserAndPermission(user, permission);
    }

    public List<String> getUserPermissions(String username) {
        return userPermissionRepository.findActivePermissionCodesByUsername(username);
    }

    public List<UserPermission> getUserPermissionDetails(String username) {
        KeycloakUser user = keycloakUserRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        return userPermissionRepository.findByUserAndActiveTrue(user);
    }

    public boolean hasPermission(String username, String permissionCode) {
        return userPermissionRepository.hasPermissionByUsername(username, permissionCode);
    }

    public boolean hasAnyPermission(String username, String... permissionCodes) {
        for (String code : permissionCodes) {
            if (hasPermission(username, code)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllPermissions(String username, String... permissionCodes) {
        for (String code : permissionCodes) {
            if (!hasPermission(username, code)) {
                return false;
            }
        }
        return true;
    }

    // Utility methods
    public Set<String> getAllUserPermissions(String username) {
        List<String> permissions = getUserPermissions(username);
        return new HashSet<>(permissions);
    }
}
