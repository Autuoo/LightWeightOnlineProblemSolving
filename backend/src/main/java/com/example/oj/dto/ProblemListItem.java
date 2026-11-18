package com.example.oj.dto;

public record ProblemListItem(
        Long id,
        String title,
        Integer difficulty,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        boolean passed,
        int submitCount,
        int acceptedCount,
        double acceptedRate) {
}