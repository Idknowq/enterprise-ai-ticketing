package com.enterprise.ticketing.auth.dto;

import com.enterprise.ticketing.auth.security.UserPrincipal;
import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String department,
        List<String> roles
) {

    public static CurrentUserResponse from(UserPrincipal userPrincipal) {
        return new CurrentUserResponse(
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getDisplayName(),
                userPrincipal.getDepartment(),
                userPrincipal.roleNames()
        );
    }
}
