package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.UserStats;
import com.example.oj.service.StatsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserStatsController {

    private final StatsService statsService;

    public UserStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public Result<UserStats> stats(Authentication authentication) {
        return Result.ok(statsService.stats((Long) authentication.getPrincipal()));
    }
}