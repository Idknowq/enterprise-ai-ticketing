package com.enterprise.ticketing.auth.context;

import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

    public Optional<UserPrincipal> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }
        return Optional.empty();
    }

    public UserPrincipal requireCurrentUser() {
        return currentUser().orElseThrow(() -> new BusinessException(ErrorCode.COMMON_UNAUTHORIZED));
    }

    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    public Optional<Long> currentUserId() {
        return currentUser().map(UserPrincipal::getId);
    }

    public boolean hasRole(SystemRole role) {
        return currentUser()
                .map(userPrincipal -> userPrincipal.getRoles().contains(role))
                .orElse(false);
    }

    public boolean hasAnyRole(SystemRole... roles) {
        for (SystemRole role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
