package com.emailmessenger.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginAuditListenerTest {

    @Mock UserActivityService activity;
    @InjectMocks LoginAuditListener listener;

    @Test
    void authenticatedSuccessEventRecordsLogin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice@example.com", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

        verify(activity).recordLogin("alice@example.com");
    }

    @Test
    void anonymousAuthenticationIsIgnored() {
        Authentication anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(anon));

        verify(activity, never()).recordLogin(anyString());
    }
}
