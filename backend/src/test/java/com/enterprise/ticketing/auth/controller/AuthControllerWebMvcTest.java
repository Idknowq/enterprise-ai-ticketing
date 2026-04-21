package com.enterprise.ticketing.auth.controller;

import com.enterprise.ticketing.auth.dto.CurrentUserResponse;
import com.enterprise.ticketing.auth.dto.LoginRequest;
import com.enterprise.ticketing.auth.dto.LoginResponse;
import com.enterprise.ticketing.auth.security.JwtAuthenticationFilter;
import com.enterprise.ticketing.auth.service.AuthService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.enterprise.ticketing.common.handler.GlobalExceptionHandler.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void loginReturnsWrappedLoginResponse() throws Exception {
        when(authService.login(eq(new LoginRequest("admin01", "ChangeMe123!"))))
                .thenReturn(new LoginResponse(
                        "jwt-token",
                        "Bearer",
                        3600,
                        Instant.parse("2026-04-19T12:00:00Z"),
                        new CurrentUserResponse(1L, "admin01", "Admin", "Platform", List.of("ADMIN"))
                ));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin01",
                                  "password": "ChangeMe123!"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_SUCCESS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.user.roles[0]").value("ADMIN"));
    }

    @Test
    void loginReturnsValidationErrorsForBlankFields() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": " ",
                                  "password": ""
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value("username must not be blank"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.password").value("password must not be blank"));
    }
}
