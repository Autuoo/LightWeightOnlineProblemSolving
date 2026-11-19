package com.example.oj.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContestDetail(
        Long id,
        String title,
        String description,
        String mode,
        String phase,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime freezeAt,
        String joinType,
        boolean published,
        boolean registered,
        boolean canRegister,
        long participantCount,
        long problemCount,
        List<ContestProblemItem> problems) {
}