package com.emailmessenger.web;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WaitlistLeaderboardIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired WaitlistEntryRepository repo;

    @AfterEach
    void cleanup() {
        repo.deleteAll();
    }

    @Test
    void leaderboardEmptyStateRendersWhenNoReferralsExist() throws Exception {
        mockMvc.perform(get("/waitlist/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Top referrers")))
                .andExpect(content().string(containsString("No referrers yet")))
                .andExpect(content().string(containsString("id=\"cookie-banner\"")));
    }

    @Test
    void leaderboardRendersTopReferrersWithAnonymizedEmails() throws Exception {
        WaitlistEntry winner = repo.save(new WaitlistEntry("winner@example.com"));
        WaitlistEntry runnerUp = repo.save(new WaitlistEntry("runnerup@example.com"));
        for (int i = 0; i < 4; i++) winner.incrementReferralsCount();
        for (int i = 0; i < 2; i++) runnerUp.incrementReferralsCount();
        repo.save(winner);
        repo.save(runnerUp);

        mockMvc.perform(get("/waitlist/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("w***@example.com")))
                .andExpect(content().string(containsString("r***@example.com")))
                .andExpect(content().string(not(containsString("winner@example.com"))))
                .andExpect(content().string(not(containsString("runnerup@example.com"))));
    }

    @Test
    void sitemapAdvertisesLeaderboardUrl() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/waitlist/leaderboard</loc>")));
    }
}
