package com.example.oj.ai.llm;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatClient implements LlmClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(properties.getTimeoutSeconds() * 1000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, List<ToolDefinition> tools) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()
                || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "未配置模型端点或密钥");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages.stream().map(this::toMessageMap).toList());
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens", properties.getMaxTokens());
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools.stream().map(this::toToolMap).toList());
            body.put("tool_choice", "auto");
        }

        String raw;
        try {
            raw = restClient.post()
                    .uri(normalizeBaseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "模型调用失败");
        }

        return parse(raw);
    }

    private LlmResponse parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode message = root.path("choices").path(0).path("message");
            String content = message.path("content").isMissingNode() || message.path("content").isNull()
                    ? null : message.path("content").asText();

            List<ToolCall> calls = new ArrayList<>();
            for (JsonNode node : message.path("tool_calls")) {
                calls.add(new ToolCall(
                        node.path("id").asText(),
                        node.path("function").path("name").asText(),
                        node.path("function").path("arguments").asText()));
            }

            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            return new LlmResponse(content, calls, promptTokens, completionTokens);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "模型响应解析失败");
        }
    }

    private Map<String, Object> toMessageMap(LlmMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", message.role());
        if (message.content() != null) {
            map.put("content", message.content());
        }
        if (message.toolCallId() != null) {
            map.put("tool_call_id", message.toolCallId());
        }
        if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
            map.put("tool_calls", message.toolCalls().stream().map(call -> {
                Map<String, Object> function = new LinkedHashMap<>();
                function.put("name", call.name());
                function.put("arguments", call.arguments());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", call.id());
                item.put("type", "function");
                item.put("function", function);
                return item;
            }).toList());
        }
        return map;
    }

    private Map<String, Object> toToolMap(ToolDefinition definition) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", definition.name());
        function.put("description", definition.description());
        function.put("parameters", definition.parameters());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "function");
        item.put("function", function);
        return item;
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl().trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}