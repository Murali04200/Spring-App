package com.example.students.repo;

import com.example.students.model.UserPermission;
import com.example.students.model.KeycloakUser;
import com.example.students.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    
    List<UserPermission> findByUserAndActiveTrue(KeycloakUser user);
    
    List<UserPermission> findByUser(KeycloakUser user);
    
    @Query("SELECT up.permission.code FROM UserPermission up WHERE up.user = :user AND up.active = true AND up.permission.active = true")
    List<String> findActivePermissionCodesByUser(@Param("user") KeycloakUser user);
    
    @Query("SELECT up.permission.code FROM UserPermission up WHERE up.user.username = :username AND up.active = true AND up.permission.active = true")
    List<String> findActivePermissionCodesByUsername(@Param("username") String username);
    
    Optional<UserPermission> findByUserAndPermission(KeycloakUser user, Permission permission);
    
    void deleteByUserAndPermission(KeycloakUser user, Permission permission);
    
    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.user = :user AND up.permission.code = :permissionCode AND up.active = true AND up.permission.active = true")
    boolean hasPermission(@Param("user") KeycloakUser user, @Param("permissionCode") String permissionCode);
    
    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.user.username = :username AND up.permission.code = :permissionCode AND up.active = true AND up.permission.active = true")
    boolean hasPermissionByUsername(@Param("username") String username, @Param("permissionCode") String permissionCode);
}
