package com.enterprise.ticketing.auth.security;

import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final ApplicationProperties applicationProperties;
    private final Clock clock = Clock.systemUTC();

    public JwtTokenProvider(ObjectMapper objectMapper, ApplicationProperties applicationProperties) {
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    void validateSecret() {
        if (applicationProperties.getAuth().getJwt().getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
    }

    public String generateToken(UserPrincipal userPrincipal) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(applicationProperties.getAuth().getJwt().getAccessTokenTtl());

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", applicationProperties.getAuth().getJwt().getIssuer());
        payload.put("sub", userPrincipal.getUsername());
        payload.put("uid", userPrincipal.getId());
        payload.put("roles", userPrincipal.roleNames());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        try {
            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String content = headerPart + "." + payloadPart;
            String signaturePart = encode(sign(content));
            return content + "." + signaturePart;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate JWT token", exception);
        }
    }

    public JwtClaims parseAndValidate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "JWT token is missing");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "JWT token format is invalid");
        }

        try {
            String signingContent = parts[0] + "." + parts[1];
            byte[] actualSignature = decode(parts[2]);
            byte[] expectedSignature = sign(signingContent);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "JWT token signature is invalid");
            }

            Map<String, Object> payload = objectMapper.readValue(decode(parts[1]), MAP_TYPE_REFERENCE);
            String issuer = asString(payload.get("iss"));
            if (!applicationProperties.getAuth().getJwt().getIssuer().equals(issuer)) {
                throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "JWT token issuer is invalid");
            }

            String username = asString(payload.get("sub"));
            Long userId = asLong(payload.get("uid"));
            Instant issuedAt = Instant.ofEpochSecond(asLong(payload.get("iat")));
            Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
            if (expiresAt.isBefore(Instant.now(clock))) {
                throw new JwtAuthenticationException(ErrorCode.AUTH_TOKEN_EXPIRED, "JWT token has expired");
            }

            Object rolesObject = payload.get("roles");
            if (!(rolesObject instanceof List<?> rolesList)) {
                throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "JWT token roles are invalid");
            }
            Set<SystemRole> roles = rolesList.stream()
                    .map(Object::toString)
                    .map(roleName -> {
                        try {
                            return SystemRole.valueOf(roleName);
                        } catch (IllegalArgumentException exception) {
                            throw new JwtAuthenticationException(
                                    ErrorCode.AUTH_INVALID_TOKEN,
                                    "JWT token contains unsupported role: " + roleName
                            );
                        }
                    })
                    .collect(Collectors.toSet());

            if (!StringUtils.hasText(username) || userId == null) {
                throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "JWT token subject is invalid");
            }

            return new JwtClaims(userId, username, roles, issuedAt, expiresAt);
        } catch (JwtAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JwtAuthenticationException(ErrorCode.AUTH_INVALID_TOKEN, "Failed to parse JWT token");
        }
    }

    public long getAccessTokenTtlSeconds() {
        return applicationProperties.getAuth().getJwt().getAccessTokenTtl().toSeconds();
    }

    private String encodeJson(Map<String, Object> content) throws Exception {
        return encode(objectMapper.writeValueAsBytes(content));
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(
                applicationProperties.getAuth().getJwt().getSecret().getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
        ));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] content) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content);
    }

    private byte[] decode(String content) {
        return Base64.getUrlDecoder().decode(content);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
