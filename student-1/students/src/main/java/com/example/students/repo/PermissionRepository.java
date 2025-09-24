package com.example.students.repo;

import com.example.students.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByCode(String code);
    
    List<Permission> findByActiveTrue();
    
    @Query("SELECT p FROM Permission p WHERE p.code IN :codes AND p.active = true")
    List<Permission> findByCodesAndActive(List<String> codes);
    
    boolean existsByCode(String code);
}
