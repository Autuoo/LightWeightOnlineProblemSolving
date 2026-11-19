package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ContestUpsertRequest(
        @NotBlank(message = "比赛标题不能为空")
        @Size(max = 128, message = "比赛标题不能超过 128 字符")
        String title,

        @Size(max = 20000, message = "比赛说明不能超过 20000 字符")
        String description,

        @NotBlank(message = "赛制不能为空")
        String mode,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime startAt,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endAt,

        LocalDateTime freezeAt,

        @NotBlank(message = "报名方式不能为空")
        String joinType,

        @Size(max = 64, message = "报名密码不能超过 64 字符")
        String password,

        Boolean published) {
}