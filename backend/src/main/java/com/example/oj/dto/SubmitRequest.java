package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRequest(
        @NotNull(message = "题目 ID 不能为空")
        Long problemId,

        @NotBlank(message = "语言不能为空")
        String language,

        @NotBlank(message = "代码不能为空")
        @Size(max = 65536, message = "代码长度不能超过 65536 字符")
        String code) {
}