package com.example.oj.ai.llm;

import java.util.List;

public record LlmMessage(String role, String content, String toolCallId, List<LlmClient.ToolCall> toolCalls) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null);
    }

    public static LlmMessage assistant(String content, List<LlmClient.ToolCall> toolCalls) {
        return new LlmMessage("assistant", content, null, toolCalls);
    }

    public static LlmMessage tool(String toolCallId, String content) {
        return new LlmMessage("tool", content, toolCallId, null);
    }
}