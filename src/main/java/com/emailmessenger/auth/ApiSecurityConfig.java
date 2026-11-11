package com.emailmessenger.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security for the mobile REST API. A separate, higher-priority chain so
 * {@code /api/**} is stateless bearer-token auth with JSON 401s, while the
 * website keeps its session/form-login/CSRF chain untouched.
 */
@Configuration
class ApiSecurityConfig {

    static final String UNAUTHORIZED_BODY =
            "{\"error\":{\"code\":\"unauthorized\",\"message\":\"Authentication is required.\"}}";

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ApiTokenService tokens) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                // No cookies are accepted here, so there is no ambient credential for CSRF to ride on.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(UNAUTHORIZED_BODY);
                }))
                .addFilterBefore(new ApiTokenAuthenticationFilter(tokens),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
