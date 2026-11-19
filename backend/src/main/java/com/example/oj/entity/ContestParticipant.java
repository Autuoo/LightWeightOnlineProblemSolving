package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_participant")
public class ContestParticipant {

    private Long contestId;
    private Long userId;
    private LocalDateTime registeredAt;
}