package com.emailmessenger.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Boot auto-registers Filter beans into the main servlet chain.
    // The throttle filter is meant to run only inside Spring Security's
    // chain (where addFilterBefore places it before the form-login
    // filter); disable the auto-registration so it isn't invoked twice
    // per request.
    @Bean
    FilterRegistrationBean<LoginThrottleFilter> loginThrottleFilterRegistration(
            LoginThrottleFilter filter) {
        FilterRegistrationBean<LoginThrottleFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    /**
     * The mobile client's REST API. Ordered ahead of the browser chain so
     * {@code /api/**} never sees form login, sessions, remember-me, or the
     * login redirect: it is stateless, authenticated only by a bearer token
     * ({@link ApiTokenAuthenticationFilter}), and answers an unauthenticated
     * call with a JSON 401. CSRF is off because nothing here is
     * cookie-authenticated — a cross-site request can't carry the token.
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               ApiTokenService apiTokenService) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                            "{\"error\":{\"code\":\"unauthorized\","
                                    + "\"message\":\"A valid bearer token is required.\"}}");
                }))
                .addFilterBefore(new ApiTokenAuthenticationFilter(apiTokenService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            PersistentTokenRepository tokenRepository,
                                            PlanCheckoutSuccessHandler planCheckoutSuccessHandler,
                                            LoginThrottleFilter loginThrottleFilter,
                                            LoginFailureHandler loginFailureHandler,
                                            ObjectProvider<ClientRegistrationRepository> oauthRegistrations,
                                            ObjectProvider<GoogleOidcUserService> googleOidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/pricing", "/demo",
                                "/login", "/register",
                                "/auth/google/start",
                                "/password/forgot", "/password/reset",
                                "/verify-email",
                                "/privacy", "/terms", "/refund",
                                "/billing/cancel", "/billing/webhook",
                                "/digest/opt-out",
                                "/robots.txt", "/sitemap.xml",
                                // Browsers (e.g. Chrome DevTools) probe /.well-known/* in the
                                // background. Permit it so an unauthenticated probe 404s directly
                                // instead of being saved as the post-login redirect target.
                                "/.well-known/**",
                                "/manifest.webmanifest", "/icons/**", "/apple-touch-icon.png",
                                "/sw.js", "/offline",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/error", "/h2-console/**",
                                "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(planCheckoutSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .tokenRepository(tokenRepository)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 30)
                        .key("conexusmail-remember-me")
                )
                // H2 console serves frames; allow same-origin in dev.
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // Spring Security enables CSRF by default; we keep it on. The H2 console
                // ships its own forms and would 403 with our CsrfToken, so exempt it.
                // Stripe webhooks POST raw JSON with a signature header, not a CSRF token.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/billing/webhook"))
                // Throttle has to run before the form-login filter so a locked
                // email never reaches credentials check.
                .addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter.class);

        // OAuth2 login wires in only when a ClientRegistrationRepository
        // bean exists (i.e. GOOGLE_CLIENT_ID env var is set). Without the
        // env vars there are no registrations and oauth2Login() would 500
        // on every page load — so we gate the whole feature behind the
        // ObjectProvider check.
        if (oauthRegistrations.getIfAvailable() != null) {
            GoogleOidcUserService oidcService = googleOidcUserService.getIfAvailable();
            http.oauth2Login(o -> {
                o.loginPage("/login")
                        .successHandler(planCheckoutSuccessHandler)
                        .failureUrl("/login?error=oauth");
                if (oidcService != null) {
                    o.userInfoEndpoint(u -> u.oidcUserService(oidcService));
                }
            });
        }

        return http.build();
    }
}
