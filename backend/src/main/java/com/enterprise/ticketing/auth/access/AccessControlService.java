package com.enterprise.ticketing.auth.access;

import com.enterprise.ticketing.auth.context.UserContext;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccessControlService {

    private final UserContext userContext;

    public AccessControlService(UserContext userContext) {
        this.userContext = userContext;
    }

    public UserPrincipal currentUser() {
        return userContext.requireCurrentUser();
    }

    public boolean hasRole(SystemRole role) {
        return userContext.hasRole(role);
    }

    public boolean hasAnyRole(SystemRole... roles) {
        return userContext.hasAnyRole(roles);
    }

    public void checkAnyRole(SystemRole... roles) {
        if (!hasAnyRole(roles)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN,
                    "Required role not granted: " + Arrays.toString(roles)
            );
        }
    }

    public boolean isCurrentUser(Long userId) {
        return userId != null && userContext.currentUserId()
                .map(currentUserId -> Objects.equals(currentUserId, userId))
                .orElse(false);
    }

    public boolean canAccessOwnedResource(Long ownerUserId, SystemRole... elevatedRoles) {
        return isCurrentUser(ownerUserId) || hasAnyRole(elevatedRoles);
    }

    public void checkOwnedResourceAccess(Long ownerUserId, SystemRole... elevatedRoles) {
        if (!canAccessOwnedResource(ownerUserId, elevatedRoles)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "Current user cannot access this resource");
        }
    }

    public boolean canAccessDepartmentResource(String department, SystemRole... elevatedRoles) {
        if (hasAnyRole(elevatedRoles)) {
            return true;
        }
        if (!StringUtils.hasText(department)) {
            return false;
        }
        return userContext.currentUser()
                .map(UserPrincipal::getDepartment)
                .filter(StringUtils::hasText)
                .map(currentDepartment -> currentDepartment.equalsIgnoreCase(department))
                .orElse(false);
    }

    public void checkDepartmentResourceAccess(String department, SystemRole... elevatedRoles) {
        if (!canAccessDepartmentResource(department, elevatedRoles)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "Current user cannot access this department resource");
        }
    }
}
