package com.example.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record HintRequest(
        @Size(max = 500, message = "问题不能超过 500 字符")
        String question,
        @Min(value = 1, message = "提示层级最小为 1")
        @Max(value = 3, message = "提示层级最大为 3")
        Integer level) {
}