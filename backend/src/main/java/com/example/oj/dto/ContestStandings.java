package com.example.oj.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContestStandings(
        Long contestId,
        String mode,
        String phase,
        boolean frozen,
        LocalDateTime freezeAt,
        LocalDateTime updatedAt,
        List<ContestStandingItem> items) {
}