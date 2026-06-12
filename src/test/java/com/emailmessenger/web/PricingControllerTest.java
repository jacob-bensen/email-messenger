package com.emailmessenger.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class PricingControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(new PricingController(new LandingProperties()))
                .setViewResolvers(viewResolver)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pricingPageReturns200AndPricingView() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing"));
    }

    @Test
    void rootRendersLandingForAnonymousVisitor() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void rootRendersLandingWhenAuthenticationIsAnonymousToken() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void rootRedirectsAuthenticatedUserToThreads() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner@example.com", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));
    }

    @Test
    void rootRedirectsToDemoWhenDemoQueryParamSet() throws Exception {
        mockMvc.perform(get("/").param("demo", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demo"));
    }

    @Test
    void rootDemoRedirectPreservesUtmSource() throws Exception {
        mockMvc.perform(get("/").param("demo", "1").param("utm_source", "producthunt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demo?utm_source=producthunt"));
    }

    @Test
    void landingOmitsDemoVideoWhenProviderNotConfigured() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(model().attributeDoesNotExist("demoVideo"));
    }

    @Test
    void landingExposesDemoVideoWhenProviderAndIdConfigured() throws Exception {
        LandingProperties props = new LandingProperties();
        props.getVideo().setProvider("youtube");
        props.getVideo().setId("dQw4w9WgXcQ");
        props.getVideo().setPosterUrl("https://cdn.example/poster.png");
        props.getVideo().setTitle("MailIM in 60s");

        InternalResourceViewResolver vr = new InternalResourceViewResolver();
        vr.setPrefix("/WEB-INF/templates/");
        vr.setSuffix(".html");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PricingController(props))
                .setViewResolvers(vr)
                .build();

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(model().attributeExists("demoVideo"));
    }

    @Test
    void rootDemoRedirectWinsOverAuthenticatedThreadsRedirect() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner@example.com", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        mockMvc.perform(get("/").param("demo", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demo"));
    }
}
