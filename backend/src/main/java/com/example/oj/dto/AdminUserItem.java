package com.example.oj.dto;

import java.time.LocalDateTime;

public record AdminUserItem(
        Long id,
        String username,
        String nickname,
        String role,
        Integer status,
        String avatarUrl,
        LocalDateTime createdAt) {
}