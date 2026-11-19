package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest")
public class Contest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String mode;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime freezeAt;
    private String joinType;
    private String passwordHash;
    private Integer published;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}