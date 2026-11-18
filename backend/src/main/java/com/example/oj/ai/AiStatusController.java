package com.example.oj.ai;

import com.example.oj.common.Result;
import com.example.oj.config.AiProperties;
import com.example.oj.dto.AiConfigView;
import com.example.oj.service.UserAiConfigService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiStatusController {

    private final AiProperties aiProperties;
    private final UserAiConfigService userAiConfigService;

    public AiStatusController(AiProperties aiProperties, UserAiConfigService userAiConfigService) {
        this.aiProperties = aiProperties;
        this.userAiConfigService = userAiConfigService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AiConfigView config = userAiConfigService.view(userId);
        boolean configured = config.hasApiKey() && !config.baseUrl().isBlank();
        boolean enabled = aiProperties.isEnabled() && config.enabled() && configured;
        return Result.ok(Map.of(
                "enabled", enabled,
                "configured", configured,
                "globalEnabled", aiProperties.isEnabled(),
                "model", config.model(),
                "dailyUserQuota", aiProperties.getDailyUserQuota(),
                "maxLevel", 3));
    }
}