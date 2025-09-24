package com.example.students.config;

import com.example.students.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class PermissionConfig {

    @Bean
    public CommandLineRunner initializePermissions(PermissionService permissionService) {
        return args -> {
            // Initialize default permissions when application starts
            permissionService.initializeDefaultPermissions();
        };
    }
}
