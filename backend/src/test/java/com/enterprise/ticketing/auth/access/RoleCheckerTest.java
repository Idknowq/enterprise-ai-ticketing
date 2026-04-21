package com.enterprise.ticketing.auth.access;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCheckerTest {

    private final RoleChecker roleChecker = new RoleChecker();

    @Test
    void hasAnyRoleReturnsFalseWhenAuthenticationMissing() {
        assertThat(roleChecker.hasAnyRole(null, "ADMIN", "APPROVER")).isFalse();
    }

    @Test
    void hasAnyRoleReturnsFalseWhenAuthenticationNotAuthenticated() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        authentication.setAuthenticated(false);

        assertThat(roleChecker.hasAnyRole(authentication, "ADMIN")).isFalse();
    }

    @Test
    void hasRoleMatchesSpringSecurityAuthorityPrefix() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPPORT_AGENT"),
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                )
        );

        assertThat(roleChecker.hasRole(authentication, "SUPPORT_AGENT")).isTrue();
        assertThat(roleChecker.hasAnyRole(authentication, "APPROVER", "EMPLOYEE")).isTrue();
        assertThat(roleChecker.hasAnyRole(authentication, "ADMIN")).isFalse();
    }
}
