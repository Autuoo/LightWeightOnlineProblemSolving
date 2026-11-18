package com.example.oj.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TestCaseBatchRequest(
        @NotEmpty(message = "测试点不能为空")
        @Valid
        List<Item> testCases) {

    public record Item(
            @NotNull(message = "序号不能为空")
            Integer orderNo,

            @NotNull(message = "输入不能为 null")
            @Size(max = 1_048_576, message = "输入不能超过 1MB")
            String input,

            @NotNull(message = "期望输出不能为 null")
            @Size(max = 1_048_576, message = "期望输出不能超过 1MB")
            String expectedOutput,

            Integer score) {
    }
}