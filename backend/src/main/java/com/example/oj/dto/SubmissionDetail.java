package com.example.oj.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionDetail(
        Long id,
        Long problemId,
        String problemTitle,
        String language,
        String status,
        Integer score,
        Integer execTimeMs,
        Integer memoryKb,
        String compileOutput,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        List<CaseView> cases) {

    public record CaseView(
            Long caseId,
            Integer orderNo,
            String status,
            Integer execTimeMs,
            Integer memoryKb) {
    }
}