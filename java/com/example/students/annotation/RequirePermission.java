package com.example.students.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * Required permission codes (e.g., "A", "B", "C")
     * User must have ALL specified permissions
     */
    String[] value() default {};
    
    /**
     * Alternative permission codes - user needs ANY of these
     * Used with anyOf parameter
     */
    String[] anyOf() default {};
    
    /**
     * If true, user needs ANY of the permissions in value()
     * If false (default), user needs ALL permissions in value()
     */
    boolean requireAny() default false;
}
