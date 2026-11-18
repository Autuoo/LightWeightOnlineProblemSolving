package com.example.oj.ai;

import com.example.oj.common.Result;
import com.example.oj.config.AiProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiStatusController {

    private final AiProperties aiProperties;

    public AiStatusController(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(Map.of(
                "enabled", aiProperties.isEnabled(),
                "model", aiProperties.getModel(),
                "dailyUserQuota", aiProperties.getDailyUserQuota(),
                "maxLevel", 3));
    }
}