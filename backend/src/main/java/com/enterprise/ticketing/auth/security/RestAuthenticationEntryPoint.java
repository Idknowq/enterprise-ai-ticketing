package com.enterprise.ticketing.auth.security;

import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.COMMON_UNAUTHORIZED;
        String message = errorCode.getDefaultMessage();

        if (authException instanceof JwtAuthenticationException jwtAuthenticationException) {
            errorCode = jwtAuthenticationException.getErrorCode();
            if (StringUtils.hasText(jwtAuthenticationException.getMessage())) {
                message = jwtAuthenticationException.getMessage();
            }
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                Result.failure(errorCode, message, TraceIdUtils.currentTraceId())
        );
    }
}
