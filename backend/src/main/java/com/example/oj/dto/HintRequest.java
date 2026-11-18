package com.example.oj.dto;

import jakarta.validation.constraints.Size;

public record HintRequest(
        @Size(max = 500, message = "问题不能超过 500 字符")
        String question) {
}