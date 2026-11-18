package com.example.oj.ai;

import java.util.List;

public record HintResult(
        String content,
        List<ToolTrace> toolTraces,
        int steps,
        int promptTokens,
        int completionTokens,
        String status) {

    public record ToolTrace(String name, boolean ok, long elapsedMs) {
    }
}