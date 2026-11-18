package com.example.oj.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiConfigRequest(
        Boolean enabled,
        @Size(max = 512, message = "端点地址不能超过 512 字符")
        String baseUrl,
        @Size(max = 64, message = "模型名不能超过 64 字符")
        String model,
        String apiKey) {
}