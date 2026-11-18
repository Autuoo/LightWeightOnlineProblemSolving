package com.example.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.jwt")
public class JwtProperties {

    private String secret = "please-change-this-secret-in-env";
    private long expireSeconds = 7200;
}