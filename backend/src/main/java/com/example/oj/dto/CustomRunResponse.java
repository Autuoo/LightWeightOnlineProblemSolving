package com.example.oj.dto;

public record CustomRunResponse(
        String status,
        int exitCode,
        long timeMs,
        long memoryKb,
        String stdout,
        String stderr,
        String compileOutput) {
}