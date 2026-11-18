package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.dto.UserStats;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.SubmissionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private static final String ACCEPTED = "AC";
    private static final int TREND_DAYS = 7;

    private final SubmissionMapper submissionMapper;

    public StatsService(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    public UserStats stats(Long userId) {
        LocalDate today = LocalDate.now();
        List<Submission> todayRows = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .ge(Submission::getCreatedAt, today.atStartOfDay())
                .lt(Submission::getCreatedAt, today.plusDays(1).atStartOfDay()));

        long submissions = todayRows.size();
        long accepted = todayRows.stream()
                .filter(item -> ACCEPTED.equals(item.getStatus()))
                .count();
        long solvedProblems = todayRows.stream()
                .filter(item -> ACCEPTED.equals(item.getStatus()))
                .map(Submission::getProblemId)
                .distinct()
                .count();
        double acceptanceRate = submissions == 0
                ? 0.0
                : Math.round(accepted * 10000.0 / submissions) / 100.0;

        LocalDate from = today.minusDays(TREND_DAYS - 1L);
        List<Submission> recent = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .ge(Submission::getCreatedAt, from.atStartOfDay()));

        Map<LocalDate, long[]> byDay = new HashMap<>();
        for (Submission item : recent) {
            if (item.getCreatedAt() == null) {
                continue;
            }
            long[] values = byDay.computeIfAbsent(item.getCreatedAt().toLocalDate(), key -> new long[2]);
            values[0]++;
            if (ACCEPTED.equals(item.getStatus())) {
                values[1]++;
            }
        }

        List<UserStats.DayPoint> trend = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate date = from.plusDays(i);
            long[] values = byDay.getOrDefault(date, new long[2]);
            trend.add(new UserStats.DayPoint(date, values[0], values[1]));
        }

        UserStats.Today summary = new UserStats.Today(submissions, accepted, acceptanceRate, solvedProblems);
        return new UserStats(summary, trend);
    }
}