package com.example.oj.ai.llm;

import java.util.List;
import java.util.Map;

public interface LlmClient {

    LlmResponse chat(LlmCredentials credentials, List<LlmMessage> messages, List<ToolDefinition> tools);

    record LlmResponse(String content, List<ToolCall> toolCalls, int promptTokens, int completionTokens) {

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    record ToolCall(String id, String name, String arguments) {
    }

    record ToolDefinition(String name, String description, Map<String, Object> parameters) {
    }
}