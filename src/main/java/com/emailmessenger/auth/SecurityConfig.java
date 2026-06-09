package com.emailmessenger.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            PersistentTokenRepository tokenRepository,
                                            PlanCheckoutSuccessHandler planCheckoutSuccessHandler,
                                            LoginThrottleFilter loginThrottleFilter,
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
                        .failureUrl("/login?error")
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
                        .key("mailim-remember-me")
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
