package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("contest_problem")
public class ContestProblem {

    private Long contestId;
    private Long problemId;
    private String alias;
    private Integer orderNo;
    private Integer score;
}