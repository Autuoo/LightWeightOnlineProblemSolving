package com.example.oj.dto;

public record SubmitResponse(Long submissionId, String status, boolean reused) {
}