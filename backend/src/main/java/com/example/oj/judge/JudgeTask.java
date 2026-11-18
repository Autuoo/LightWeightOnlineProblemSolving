package com.example.oj.judge;

public record JudgeTask(Long submissionId, Long problemId, String language, String code) {
}