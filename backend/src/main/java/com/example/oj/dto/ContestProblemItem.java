package com.example.oj.dto;

public record ContestProblemItem(
        Long problemId,
        String alias,
        Integer orderNo,
        String title,
        Integer difficulty,
        boolean passed,
        Integer score) {
}