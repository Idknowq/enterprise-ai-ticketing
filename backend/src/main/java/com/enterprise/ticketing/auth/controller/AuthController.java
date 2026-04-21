package com.enterprise.ticketing.auth.controller;

import com.enterprise.ticketing.auth.dto.CurrentUserResponse;
import com.enterprise.ticketing.auth.dto.LoginRequest;
import com.enterprise.ticketing.auth.dto.LoginResponse;
import com.enterprise.ticketing.auth.service.AuthService;
import com.enterprise.ticketing.common.api.OpenApiResultSchemas;
import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication and authorization APIs")
@RestController
@RequestMapping("${app.api-base-path:/api}/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Authenticate a platform user and return a JWT access token.")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Current user", description = "Return the current authenticated user from the JWT context.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OpenApiResultSchemas.CurrentUserResult.class)))
    @GetMapping("/me")
    public Result<CurrentUserResponse> currentUser() {
        return Result.success(authService.currentUser(), TraceIdUtils.currentTraceId());
    }
}
