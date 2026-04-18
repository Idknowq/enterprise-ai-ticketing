package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.auth.context.UserContext;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DocumentAccessPolicy {

    private final UserContext userContext;
    private final ApplicationProperties applicationProperties;

    public DocumentAccessPolicy(UserContext userContext, ApplicationProperties applicationProperties) {
        this.userContext = userContext;
        this.applicationProperties = applicationProperties;
    }

    public UserPrincipal currentUser() {
        return userContext.requireCurrentUser();
    }

    public void checkDocumentManagementAccess() {
        if (!userContext.hasAnyRole(SystemRole.ADMIN, SystemRole.SUPPORT_AGENT)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "Current user cannot manage knowledge documents");
        }
    }

    public Set<KnowledgeAccessLevel> allowedAccessLevels() {
        if (userContext.hasRole(SystemRole.ADMIN)) {
            return KnowledgeAccessLevel.allLevels();
        }
        if (userContext.hasAnyRole(SystemRole.SUPPORT_AGENT, SystemRole.APPROVER)) {
            return KnowledgeAccessLevel.elevatedLevels();
        }
        return KnowledgeAccessLevel.employeeLevels();
    }

    public Set<String> allowedDepartments(String requestedDepartment) {
        String globalDepartment = normalizeDepartment(applicationProperties.getKnowledge().getGlobalDepartment());
        if (userContext.hasAnyRole(SystemRole.ADMIN, SystemRole.SUPPORT_AGENT)) {
            if (StringUtils.hasText(requestedDepartment)) {
                return Set.of(normalizeDepartment(requestedDepartment));
            }
            return Set.of();
        }

        Set<String> allowed = new LinkedHashSet<>();
        allowed.add(globalDepartment);
        userContext.currentUser()
                .map(UserPrincipal::getDepartment)
                .filter(StringUtils::hasText)
                .map(this::normalizeDepartment)
                .ifPresent(allowed::add);

        if (!StringUtils.hasText(requestedDepartment)) {
            return allowed;
        }

        String requested = normalizeDepartment(requestedDepartment);
        return allowed.contains(requested) ? Set.of(requested) : Set.of("__DENY__");
    }

    public String normalizeDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return applicationProperties.getKnowledge().getGlobalDepartment();
        }
        return department.trim().toUpperCase();
    }

    public String normalizeCategory(String category) {
        return category == null ? null : category.trim().toUpperCase();
    }
}
