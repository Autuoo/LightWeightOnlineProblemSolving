package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.dto.UserStats;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.SubmissionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StatsService {

    private static final String ACCEPTED = "AC";
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    private final SubmissionMapper submissionMapper;

    public StatsService(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    public UserStats stats(Long userId, int days) {
        int range = Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(range - 1L);

        List<Submission> recent = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .ge(Submission::getCreatedAt, from.atStartOfDay())
                .lt(Submission::getCreatedAt, today.plusDays(1).atStartOfDay()));

        Map<LocalDate, DayAccumulator> byDay = new HashMap<>();
        for (Submission item : recent) {
            if (item.getCreatedAt() == null) {
                continue;
            }
            byDay.computeIfAbsent(item.getCreatedAt().toLocalDate(), key -> new DayAccumulator())
                    .add(item);
        }

        List<UserStats.DayPoint> trend = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            LocalDate date = from.plusDays(i);
            DayAccumulator day = byDay.getOrDefault(date, new DayAccumulator());
            trend.add(new UserStats.DayPoint(date, day.submissions, day.accepted, day.solvedProblems.size()));
        }

        DayAccumulator todayData = byDay.getOrDefault(today, new DayAccumulator());
        double rate = todayData.submissions == 0
                ? 0.0
                : Math.round(todayData.accepted * 10000.0 / todayData.submissions) / 100.0;
        UserStats.Today summary = new UserStats.Today(
                todayData.submissions, todayData.accepted, rate, todayData.solvedProblems.size());
        return new UserStats(summary, trend);
    }

    private static final class DayAccumulator {
        private long submissions;
        private long accepted;
        private final Set<Long> solvedProblems = new HashSet<>();

        void add(Submission item) {
            submissions++;
            if (ACCEPTED.equals(item.getStatus())) {
                accepted++;
                solvedProblems.add(item.getProblemId());
            }
        }
    }
}