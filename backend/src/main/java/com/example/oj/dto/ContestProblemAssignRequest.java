package com.example.oj.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ContestProblemAssignRequest(
        @NotEmpty(message = "比赛至少需要一道题目")
        @Valid
        List<Item> problems) {

    public record Item(
            @NotNull(message = "题目 ID 不能为空")
            Long problemId,
            String alias,
            Integer orderNo,
            Integer score) {
    }
}