package com.example.oj.dto;

import java.time.LocalDateTime;

public record ProfileView(
        Long id,
        String username,
        String nickname,
        String role,
        String avatarUrl,
        LocalDateTime createdAt) {
}