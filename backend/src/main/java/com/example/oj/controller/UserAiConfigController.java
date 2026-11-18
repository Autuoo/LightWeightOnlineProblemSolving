package com.example.oj.controller;

import com.example.oj.ai.llm.LlmClient;
import com.example.oj.ai.llm.LlmCredentials;
import com.example.oj.ai.llm.LlmMessage;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.common.Result;
import com.example.oj.config.AiProperties;
import com.example.oj.dto.AiConfigRequest;
import com.example.oj.dto.AiConfigView;
import com.example.oj.service.UserAiConfigService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me/ai-config")
public class UserAiConfigController {

    private final UserAiConfigService userAiConfigService;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;

    public UserAiConfigController(UserAiConfigService userAiConfigService,
                                  LlmClient llmClient,
                                  AiProperties aiProperties) {
        this.userAiConfigService = userAiConfigService;
        this.llmClient = llmClient;
        this.aiProperties = aiProperties;
    }

    @GetMapping
    public Result<AiConfigView> get(Authentication authentication) {
        return Result.ok(userAiConfigService.view((Long) authentication.getPrincipal()));
    }

    @PutMapping
    public Result<AiConfigView> save(@Valid @RequestBody AiConfigRequest request,
                                     Authentication authentication) {
        return Result.ok(userAiConfigService.save((Long) authentication.getPrincipal(), request));
    }

    @PostMapping("/test")
    public Result<Map<String, Object>> test(Authentication authentication) {
        if (!aiProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 助教未启用");
        }
        LlmCredentials credentials = userAiConfigService.resolve((Long) authentication.getPrincipal());
        if (credentials == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先保存端点与 API Key");
        }
        long start = System.currentTimeMillis();
        try {
            llmClient.chat(credentials, List.of(LlmMessage.user("只回复 OK")), List.of());
            return Result.ok(Map.of(
                    "ok", true,
                    "model", credentials.model(),
                    "elapsedMs", System.currentTimeMillis() - start));
        } catch (BusinessException ex) {
            return Result.ok(Map.of(
                    "ok", false,
                    "message", ex.getMessage(),
                    "elapsedMs", System.currentTimeMillis() - start));
        }
    }
}