package com.emailmessenger.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The stateless bearer-token chain for the mobile REST API. It claims
 * {@code /api/**} ahead of the browser chain in {@link SecurityConfig}, so API
 * requests never get a session, a CSRF check, or a redirect to the login page —
 * an unauthenticated call is answered with a JSON 401 instead.
 */
@Configuration
class ApiSecurityConfig {

    static final String UNAUTHORIZED_BODY =
            "{\"error\":{\"code\":\"unauthorized\",\"message\":\"Sign in to continue.\"}}";
    static final String FORBIDDEN_BODY =
            "{\"error\":{\"code\":\"forbidden\",\"message\":\"You don't have access to that.\"}}";

    // The global manager (DbUserDetailsService + BCrypt) that form login also
    // uses. It publishes the success/failure events LoginAuditListener and
    // AuthFailureListener consume, so API logins are audited and throttled
    // exactly like browser logins.
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ApiTokenService tokens) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                // Bearer tokens aren't sent automatically by a browser, so there is
                // no ambient credential for a cross-site request to ride on.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, HttpStatus.FORBIDDEN, FORBIDDEN_BODY)))
                .addFilterBefore(new ApiTokenAuthenticationFilter(tokens),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeJson(jakarta.servlet.http.HttpServletResponse response,
                                  HttpStatus status, String body) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}
