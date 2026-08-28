package com.emailmessenger.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Second Spring Security filter chain, scoped to {@code /api/v1/**}. Runs
 * before the form-login chain (via {@link Order @Order(1)}) so bearer-token
 * requests never touch the session-cookie / CSRF machinery meant for the
 * Thymeleaf website.
 */
@Configuration
class ApiSecurityConfig {

    /**
     * Prevent Spring Boot from also adding the API-token filter to the
     * main servlet chain — we install it inside this security chain via
     * {@code addFilterBefore(...)} and don't want it to double-fire.
     */
    @Bean
    FilterRegistrationBean<ApiTokenAuthenticationFilter> apiTokenFilterRegistration(
            ApiTokenAuthenticationFilter filter) {
        FilterRegistrationBean<ApiTokenAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    ApiTokenAuthenticationFilter apiTokenAuthenticationFilter(ApiTokenService tokens) {
        return new ApiTokenAuthenticationFilter(tokens);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               ApiTokenAuthenticationFilter apiTokenFilter,
                                               ObjectMapper objectMapper) throws Exception {
        http
                .securityMatcher("/api/v1/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, resp, e) ->
                                writeError(objectMapper, resp, HttpServletResponse.SC_UNAUTHORIZED,
                                        "unauthorized"))
                        .accessDeniedHandler((req, resp, e) ->
                                writeError(objectMapper, resp, HttpServletResponse.SC_FORBIDDEN,
                                        "forbidden")))
                .addFilterBefore(apiTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(ObjectMapper objectMapper,
                                   HttpServletResponse resp,
                                   int status,
                                   String slug) throws java.io.IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(resp.getOutputStream(), ApiErrorResponse.of(slug));
    }
}
