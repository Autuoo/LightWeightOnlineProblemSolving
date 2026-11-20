package com.example.oj.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 32, message = "昵称不能超过 32 个字符")
        String nickname,
        @Pattern(regexp = "^(ADMIN|STUDENT)$", message = "角色只能是 ADMIN 或 STUDENT")
        String role) {
}