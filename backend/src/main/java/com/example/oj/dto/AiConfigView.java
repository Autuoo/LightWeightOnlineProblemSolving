package com.example.oj.dto;

import java.time.LocalDateTime;

public record AiConfigView(
        boolean enabled,
        String baseUrl,
        String model,
        boolean hasApiKey,
        String apiKeyMasked,
        LocalDateTime updatedAt) {
}