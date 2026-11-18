package com.example.oj.ai.tool;

import com.example.oj.ai.HintContext;
import com.example.oj.ai.HintTool;
import com.example.oj.config.JudgeProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetLanguageLimitsTool implements HintTool {

    private final JudgeProperties judgeProperties;

    public GetLanguageLimitsTool(JudgeProperties judgeProperties) {
        this.judgeProperties = judgeProperties;
    }

    @Override
    public String name() {
        return "get_language_limits";
    }

    @Override
    public String description() {
        return "获取各编程语言的时间与内存倍率，用于判断是否存在语言相关的时限差异。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.<String, Object>of(
                "type", "object",
                "properties", Map.of("language", Map.of("type", "string", "description", "语言标识")),
                "required", List.of("language"));
    }

    @Override
    public String execute(Map<String, Object> arguments, HintContext context) {
        Object language = arguments == null ? null : arguments.get("language");
        if (!(language instanceof String lang)) {
            return "时间倍率：" + judgeProperties.getTimeMultiplier();
        }
        Double multiplier = judgeProperties.getTimeMultiplier().getOrDefault(lang, 1.0);
        return lang + " 的时间倍率为 " + multiplier + "（仅作用于时间，不作用于内存）";
    }
}