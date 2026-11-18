package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_case_result")
public class SubmissionCaseResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long caseId;
    private String status;
    private Integer execTimeMs;
    private Integer memoryKb;
    private String message;
    private LocalDateTime createdAt;
}