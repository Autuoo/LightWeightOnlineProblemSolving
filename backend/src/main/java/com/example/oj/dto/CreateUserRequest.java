package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名只能包含字母、数字、下划线，长度 3-32")
        String username,
        @Size(max = 32, message = "昵称不能超过 32 个字符")
        String nickname,
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
        String password) {
}