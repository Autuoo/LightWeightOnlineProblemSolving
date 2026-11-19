package com.example.oj.dto;

import java.time.LocalDateTime;

public record ContestListItem(
        Long id,
        String title,
        String mode,
        String phase,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime freezeAt,
        String joinType,
        boolean published,
        boolean registered,
        long participantCount,
        long problemCount) {
}