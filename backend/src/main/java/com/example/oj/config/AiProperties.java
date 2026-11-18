package com.example.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.ai")
public class AiProperties {

    private boolean enabled = false;
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int maxSteps = 3;
    private int timeoutSeconds = 30;
    private int maxTokens = 800;
    private double temperature = 0.2;
    private int concurrency = 2;
    private int dailyUserQuota = 100;
    private int dailyGlobalQuota = 500;
    private int minIntervalSeconds = 0;
}