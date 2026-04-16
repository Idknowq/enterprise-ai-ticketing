package com.enterprise.ticketing.auth.service;

import com.enterprise.ticketing.auth.context.UserContext;
import com.enterprise.ticketing.auth.dto.CurrentUserResponse;
import com.enterprise.ticketing.auth.dto.LoginRequest;
import com.enterprise.ticketing.auth.dto.LoginResponse;
import com.enterprise.ticketing.auth.security.JwtTokenProvider;
import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserContext userContext;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserContext userContext
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userContext = userContext;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateToken(userPrincipal);
            long expiresInSeconds = jwtTokenProvider.getAccessTokenTtlSeconds();
            Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

            return new LoginResponse(
                    accessToken,
                    "Bearer",
                    expiresInSeconds,
                    expiresAt,
                    CurrentUserResponse.from(userPrincipal)
            );
        } catch (BadCredentialsException exception) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid username or password");
        } catch (DisabledException exception) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED, "Current user is disabled");
        }
    }

    @Override
    public CurrentUserResponse currentUser() {
        return CurrentUserResponse.from(userContext.requireCurrentUser());
    }
}
