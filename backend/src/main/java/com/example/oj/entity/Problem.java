package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("problem")
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String samples;
    private Integer difficulty;
    private String tags;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Integer visible;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}