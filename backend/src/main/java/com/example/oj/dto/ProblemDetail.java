package com.example.oj.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public record ProblemDetail(
        Long id,
        String title,
        String description,
        Integer difficulty,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        List<String> languages,
        Map<String, Double> timeMultiplier,
        JsonNode samples) {
}