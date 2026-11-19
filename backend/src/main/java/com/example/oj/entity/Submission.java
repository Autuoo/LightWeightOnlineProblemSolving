package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long problemId;
    private Long contestId;
    private String language;
    private String code;
    private String codeHash;
    private String status;
    private Integer score;
    private Integer execTimeMs;
    private Integer memoryKb;
    private String compileOutput;
    private String errorMessage;
    private Integer suspicious;
    private String suspiciousReason;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}