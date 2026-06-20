package com.emailmessenger.web;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full Spring context with a YouTube demo-video override and
 * verifies the landing hero swaps the static chat-bubble mock for the
 * click-to-play facade — proving Master can drop in a Loom/YouTube URL
 * via env at deploy time with no template change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "marketing.landing.video.provider=youtube",
        "marketing.landing.video.id=dQw4w9WgXcQ",
        "marketing.landing.video.poster-url=https://cdn.example/poster.png",
        "marketing.landing.video.title=ConexusMail in 60 seconds"
})
class LandingHeroVideoIntegrationTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean BillingService billingService;

    @Test
    void landingRendersVideoFacadeWhenConfigured() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Embed wrapper + click-to-play button render with the right URL
        // (privacy-enhanced YouTube host) and accessible title.
        assertThat(body).contains("class=\"landing-video\"");
        assertThat(body).contains("class=\"landing-video-wrap\"");
        assertThat(body).contains("data-embed-url=\"https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(body).contains("rel=0");
        assertThat(body).contains("autoplay=1");
        // The visible label + aria label use the configured human title.
        assertThat(body).contains("ConexusMail in 60 seconds");
        // Poster image is set as background so the iframe is only loaded on click.
        assertThat(body).contains("background-image:url(https://cdn.example/poster.png)");
        // The static fallback mock must NOT render simultaneously — otherwise the
        // hero would show both the video and a chat-bubble preview at once.
        assertThat(body).doesNotContain("class=\"landing-screenshot\"");
        assertThat(body).doesNotContain("class=\"screenshot-mock\"");
    }
}
