package com.example.oj.dto;

import java.time.LocalDateTime;

public record SubmissionListItem(
        Long id,
        Long problemId,
        String problemTitle,
        String language,
        String status,
        Integer score,
        Integer execTimeMs,
        Integer memoryKb,
        LocalDateTime createdAt,
        LocalDateTime finishedAt) {
}