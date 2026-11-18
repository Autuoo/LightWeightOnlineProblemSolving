package com.example.oj.dto;

import java.util.List;

public record AdminProblemDetail(
        Long id,
        String title,
        String description,
        List<ProblemUpsertRequest.Sample> samples,
        Integer difficulty,
        String tags,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer visible,
        List<CaseView> testCases) {

    public record CaseView(
            Integer orderNo,
            String input,
            String expectedOutput,
            Integer score) {
    }
}