package com.emailmessenger.web;

import com.emailmessenger.service.WaitlistLeaderboardService;
import com.emailmessenger.service.WaitlistLeaderboardService.LeaderboardEntry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
class WaitlistLeaderboardController {

    private final WaitlistLeaderboardService leaderboardService;

    WaitlistLeaderboardController(WaitlistLeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/waitlist/leaderboard")
    String show(Model model) {
        List<LeaderboardEntry> entries = leaderboardService.top10();
        model.addAttribute("entries", entries);
        return "waitlist-leaderboard";
    }
}
