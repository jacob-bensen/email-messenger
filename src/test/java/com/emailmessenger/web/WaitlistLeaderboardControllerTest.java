package com.emailmessenger.web;

import com.emailmessenger.service.WaitlistLeaderboardService;
import com.emailmessenger.service.WaitlistLeaderboardService.LeaderboardEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WaitlistLeaderboardControllerTest {

    private MockMvc mockMvc;
    private WaitlistLeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = mock(WaitlistLeaderboardService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WaitlistLeaderboardController(leaderboardService))
                .build();
    }

    @Test
    void getReturnsLeaderboardViewWithEntriesFromService() throws Exception {
        List<LeaderboardEntry> rows = List.of(
                new LeaderboardEntry(1, "a***@example.com", 7),
                new LeaderboardEntry(2, "b***@example.com", 3));
        when(leaderboardService.top10()).thenReturn(rows);

        mockMvc.perform(get("/waitlist/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist-leaderboard"))
                .andExpect(model().attribute("entries", rows));
    }

    @Test
    void getReturnsLeaderboardViewWithEmptyEntriesWhenServiceReturnsEmpty() throws Exception {
        when(leaderboardService.top10()).thenReturn(List.of());

        mockMvc.perform(get("/waitlist/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("waitlist-leaderboard"))
                .andExpect(model().attribute("entries", List.of()));
    }
}
