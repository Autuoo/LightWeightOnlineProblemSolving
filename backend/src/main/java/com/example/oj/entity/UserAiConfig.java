package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_ai_config")
public class UserAiConfig {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Integer enabled;
    private String baseUrl;
    private String apiKeyEnc;
    private String model;
    private LocalDateTime updatedAt;
}