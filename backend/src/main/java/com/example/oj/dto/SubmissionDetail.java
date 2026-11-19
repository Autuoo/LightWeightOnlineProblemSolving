package com.example.oj.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionDetail(
        Long id,
        Long problemId,
        Long contestId,
        String problemTitle,
        String language,
        String code,
        String status,
        Integer score,
        Integer execTimeMs,
        Integer memoryKb,
        String compileOutput,
        String errorMessage,
        boolean suspicious,
        String suspiciousReason,
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        boolean owner,
        List<CaseView> cases) {

    public record CaseView(
            Long caseId,
            Integer orderNo,
            String status,
            Integer execTimeMs,
            Integer memoryKb) {
    }
}