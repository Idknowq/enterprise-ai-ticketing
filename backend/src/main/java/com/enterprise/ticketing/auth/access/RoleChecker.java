package com.enterprise.ticketing.auth.access;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("roleChecker")
public class RoleChecker {

    public boolean hasRole(Authentication authentication, String role) {
        return hasAnyRole(authentication, role);
    }

    public boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String role : roles) {
            if (authorities.contains("ROLE_" + role)) {
                return true;
            }
        }
        return false;
    }
}
