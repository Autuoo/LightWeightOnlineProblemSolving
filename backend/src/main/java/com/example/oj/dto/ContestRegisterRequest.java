package com.example.oj.dto;

import jakarta.validation.constraints.Size;

public record ContestRegisterRequest(
        @Size(max = 64, message = "报名密码不能超过 64 字符")
        String password) {
}