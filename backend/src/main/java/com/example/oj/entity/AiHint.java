package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_hint")
public class AiHint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long userId;
    private String question;
    private Integer level;
    private String answer;
    private String toolCalls;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer stepCount;
    private String status;
    private LocalDateTime createdAt;
}