package com.example.oj.judge.sandbox;

public record SandboxSpec(
        String language,
        String sourceCode,
        String stdin,
        int timeLimitMs,
        int memoryLimitMb) {
}