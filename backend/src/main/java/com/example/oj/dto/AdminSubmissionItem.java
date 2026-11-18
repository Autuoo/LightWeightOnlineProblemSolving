package com.example.oj.dto;

import java.time.LocalDateTime;

public record AdminSubmissionItem(
        Long id,
        Long userId,
        String username,
        String problemTitle,
        String language,
        String status,
        Integer score,
        LocalDateTime createdAt,
        boolean suspicious) {
}