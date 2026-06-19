package com.emailmessenger.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class LegalControllerTest {

    private final LegalProperties properties = configuredProperties();
    private final StubResourceLoader resourceLoader = new StubResourceLoader();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new LegalController(properties, resourceLoader))
            .setViewResolvers(htmlViewResolver())
            .build();

    @Test
    void privacyRendersConfiguredResourceWithSeoMetadata() throws Exception {
        resourceLoader.put("classpath:legal/privacy.html",
                "<p>Privacy boilerplate body.</p>");

        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal"))
                .andExpect(model().attribute("pageTitle", "Privacy Policy — ConexusMail"))
                .andExpect(model().attribute("pagePath", "/privacy"))
                .andExpect(model().attribute("content", "<p>Privacy boilerplate body.</p>"));
    }

    @Test
    void termsRendersConfiguredResourceWithSeoMetadata() throws Exception {
        resourceLoader.put("classpath:legal/terms.html",
                "<p>Terms boilerplate body.</p>");

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal"))
                .andExpect(model().attribute("pageTitle", "Terms of Service — ConexusMail"))
                .andExpect(model().attribute("pagePath", "/terms"))
                .andExpect(model().attribute("content", "<p>Terms boilerplate body.</p>"));
    }

    @Test
    void refundRendersConfiguredResourceWithSeoMetadata() throws Exception {
        resourceLoader.put("classpath:legal/refund.html",
                "<p>Refund boilerplate body.</p>");

        mockMvc.perform(get("/refund"))
                .andExpect(status().isOk())
                .andExpect(view().name("legal"))
                .andExpect(model().attribute("pageTitle", "Refund Policy — ConexusMail"))
                .andExpect(model().attribute("pagePath", "/refund"))
                .andExpect(model().attribute("content", "<p>Refund boilerplate body.</p>"));
    }

    @Test
    void privacyHonorsOverridenResourceLocation() throws Exception {
        // Master sets MARKETING_LEGAL_PRIVACY to a deploy-time file/URL —
        // the controller must read THAT, not the classpath default.
        properties.setPrivacy("file:/etc/conexusmail/custom-privacy.html");
        resourceLoader.put("file:/etc/conexusmail/custom-privacy.html",
                "<p>Custom privacy from Termly export.</p>");

        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("content",
                        "<p>Custom privacy from Termly export.</p>"));
    }

    @Test
    void allThreeRoutesAdvertiseDistinctTitlesAndPaths() throws Exception {
        resourceLoader.put("classpath:legal/privacy.html", "<p>p</p>");
        resourceLoader.put("classpath:legal/terms.html", "<p>t</p>");
        resourceLoader.put("classpath:legal/refund.html", "<p>r</p>");

        Map<String, String> titles = new HashMap<>();
        titles.put("/privacy", (String) mockMvc.perform(get("/privacy"))
                .andReturn().getModelAndView().getModel().get("pageTitle"));
        titles.put("/terms", (String) mockMvc.perform(get("/terms"))
                .andReturn().getModelAndView().getModel().get("pageTitle"));
        titles.put("/refund", (String) mockMvc.perform(get("/refund"))
                .andReturn().getModelAndView().getModel().get("pageTitle"));

        assertThat(titles.values()).doesNotHaveDuplicates();
        assertThat(titles.values()).allMatch(t -> t.endsWith(" — ConexusMail"));
    }

    private static LegalProperties configuredProperties() {
        LegalProperties p = new LegalProperties();
        p.setPrivacy("classpath:legal/privacy.html");
        p.setTerms("classpath:legal/terms.html");
        p.setRefund("classpath:legal/refund.html");
        return p;
    }

    private static InternalResourceViewResolver htmlViewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");
        return viewResolver;
    }

    /**
     * Minimal in-memory ResourceLoader: each test seeds the locations it
     * cares about. Misses come back as a non-existent resource, which
     * the controller surfaces as a 500.
     */
    private static final class StubResourceLoader implements ResourceLoader {
        private final Map<String, String> bodies = new HashMap<>();

        void put(String location, String body) {
            bodies.put(location, body);
        }

        @Override
        public Resource getResource(String location) {
            String body = bodies.get(location);
            if (body == null) {
                return new ByteArrayResource(new byte[0]) {
                    @Override public boolean exists() { return false; }
                };
            }
            return new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }
}
