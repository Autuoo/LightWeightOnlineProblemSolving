package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_case")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private Integer orderNo;
    private String input;
    private String expectedOutput;
    private Integer score;
    private LocalDateTime createdAt;
}