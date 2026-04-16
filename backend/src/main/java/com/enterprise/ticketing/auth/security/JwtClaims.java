package com.enterprise.ticketing.auth.security;

import com.enterprise.ticketing.auth.domain.SystemRole;
import java.time.Instant;
import java.util.Set;

public record JwtClaims(
        Long userId,
        String username,
        Set<SystemRole> roles,
        Instant issuedAt,
        Instant expiresAt
) {
}
