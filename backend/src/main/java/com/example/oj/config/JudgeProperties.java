package com.example.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "oj.judge")
public class JudgeProperties {

    private int concurrency = 4;
    private int queueCapacity = 200;
    private int idempotentWindowMinutes = 10;
    private int compileTimeoutSeconds = 20;
    private int compileOutputLimitBytes = 8192;
    private Map<String, Double> timeMultiplier = new HashMap<>();
}