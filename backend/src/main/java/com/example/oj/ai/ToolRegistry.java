package com.example.oj.ai;

import com.example.oj.ai.llm.LlmClient;
import com.example.oj.common.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, HintTool> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;

    public ToolRegistry(List<HintTool> hintTools, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (HintTool tool : hintTools) {
            tools.put(tool.name(), tool);
        }
    }

    public List<LlmClient.ToolDefinition> schemas() {
        return tools.values().stream()
                .map(tool -> new LlmClient.ToolDefinition(tool.name(), tool.description(), tool.parameters()))
                .toList();
    }

    public boolean isRegistered(String name) {
        return tools.containsKey(name);
    }

    public ToolExecution execute(LlmClient.ToolCall call, HintContext context) {
        HintTool tool = tools.get(call.name());
        if (tool == null) {
            return new ToolExecution(call.name(), false, "工具不存在：" + call.name(), 0);
        }
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> arguments = objectMapper.readValue(
                    call.arguments() == null || call.arguments().isBlank() ? "{}" : call.arguments(),
                    new TypeReference<Map<String, Object>>() {
                    });
            String output = tool.execute(arguments, context);
            return new ToolExecution(call.name(), true, output, System.currentTimeMillis() - start);
        } catch (BusinessException ex) {
            return new ToolExecution(call.name(), false, ex.getMessage(), System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return new ToolExecution(call.name(), false, "工具执行失败", System.currentTimeMillis() - start);
        }
    }

    public record ToolExecution(String name, boolean ok, String output, long elapsedMs) {
    }
}