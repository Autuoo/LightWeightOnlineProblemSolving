package com.example.oj.dto;

import java.time.LocalDateTime;

public record AdminProblemItem(
        Long id,
        String title,
        Integer difficulty,
        Integer visible,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        int caseCount,
        LocalDateTime createdAt) {
}