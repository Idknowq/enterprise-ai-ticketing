package com.enterprise.ticketing.config;

import com.enterprise.ticketing.auth.security.JwtAuthenticationFilter;
import com.enterprise.ticketing.auth.security.RestAccessDeniedHandler;
import com.enterprise.ticketing.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityBaselineConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApplicationProperties applicationProperties,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        String apiBasePath = normalizeBasePath(applicationProperties.getApiBasePath());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/favicon.ico",
                                "/error",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, apiBasePath + "/platform/info").permitAll()
                        .requestMatchers(HttpMethod.POST, apiBasePath + "/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        return restAuthenticationEntryPoint;
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(RestAccessDeniedHandler restAccessDeniedHandler) {
        return restAccessDeniedHandler;
    }

    private String normalizeBasePath(String apiBasePath) {
        if (apiBasePath == null || apiBasePath.isBlank()) {
            return "/api";
        }
        return apiBasePath.startsWith("/") ? apiBasePath : "/" + apiBasePath;
    }
}
