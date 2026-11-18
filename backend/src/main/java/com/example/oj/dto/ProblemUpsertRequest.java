package com.example.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProblemUpsertRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 128, message = "标题不能超过 128 字符")
        String title,

        @NotBlank(message = "题面不能为空")
        String description,

        List<Sample> samples,

        @Min(value = 1, message = "难度取值为 1-3")
        @Max(value = 3, message = "难度取值为 1-3")
        Integer difficulty,

        @Min(value = 100, message = "时间限制不能小于 100ms")
        Integer timeLimitMs,

        @Min(value = 16, message = "内存限制不能小于 16MB")
        Integer memoryLimitMb,

        Integer visible) {

    public record Sample(String input, String output) {
    }
}