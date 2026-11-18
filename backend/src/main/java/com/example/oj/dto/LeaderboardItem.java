package com.example.oj.dto;

public record LeaderboardItem(
        int rank,
        Long userId,
        String username,
        String nickname,
        int solvedCount,
        int submissionCount,
        int acceptedCount,
        double acceptedRate) {
}