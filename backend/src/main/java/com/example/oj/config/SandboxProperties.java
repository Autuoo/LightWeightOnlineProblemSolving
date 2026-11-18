package com.example.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.sandbox")
public class SandboxProperties {

    private String type = "process";
    private String dockerImage = "oj-runner:latest";
    private String workDir = "./oj-sandbox";
}