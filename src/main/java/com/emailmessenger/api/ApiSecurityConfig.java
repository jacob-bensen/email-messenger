package com.emailmessenger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The {@code /api/**} security chain: stateless bearer-token auth for the
 * mobile client. It runs before the website's session/form-login chain and
 * never creates a session, so CSRF (a cookie-borne-credential attack) does
 * not apply here.
 */
@Configuration
class ApiSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ApiTokenService tokens,
                                               ObjectMapper objectMapper) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        // The SSE stream (§5.24) was authorized on its initial
                        // request; its async dispatches carry no bearer filter pass.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .addFilterBefore(new BearerTokenAuthenticationFilter(tokens), AnonymousAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, objectMapper,
                                HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in to continue."))
                        .accessDeniedHandler((req, res, ex) -> writeError(res, objectMapper,
                                HttpStatus.FORBIDDEN, "forbidden", "You can't do that.")));
        return http.build();
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                                   HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(code, message));
    }
}
