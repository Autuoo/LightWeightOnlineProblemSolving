package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.LeaderboardItem;
import com.example.oj.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public Result<List<LeaderboardItem>> list(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(leaderboardService.list(limit));
    }
}