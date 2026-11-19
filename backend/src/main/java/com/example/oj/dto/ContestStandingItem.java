package com.example.oj.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ContestStandingItem(
        int rank,
        Long userId,
        String username,
        String nickname,
        int solved,
        int penalty,
        LocalDateTime lastAcAt,
        Map<String, String> problems) {
}