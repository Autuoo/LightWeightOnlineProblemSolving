package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.dto.UserStats;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final String ACCEPTED = "AC";
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;

    public StatsService(SubmissionMapper submissionMapper, ProblemMapper problemMapper) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
    }

    public UserStats stats(Long userId, int days) {
        int range = Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(range - 1L);

        List<Submission> recent = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .ge(Submission::getCreatedAt, from.atStartOfDay())
                .lt(Submission::getCreatedAt, today.plusDays(1).atStartOfDay()));

        Map<LocalDate, StatAccumulator> byDay = new HashMap<>();
        for (Submission item : recent) {
            if (item.getCreatedAt() == null) {
                continue;
            }
            byDay.computeIfAbsent(item.getCreatedAt().toLocalDate(), key -> new StatAccumulator())
                    .add(item);
        }

        List<UserStats.DayPoint> trend = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            LocalDate date = from.plusDays(i);
            StatAccumulator day = byDay.getOrDefault(date, new StatAccumulator());
            trend.add(new UserStats.DayPoint(
                    date, day.submissions, day.accepted, day.solvedProblems.size()));
        }

        StatAccumulator todayData = byDay.getOrDefault(today, new StatAccumulator());
        double rate = todayData.submissions == 0
                ? 0.0
                : Math.round(todayData.accepted * 10000.0 / todayData.submissions) / 100.0;
        UserStats.Today summary = new UserStats.Today(
                todayData.submissions, todayData.accepted, rate, todayData.solvedProblems.size());

        return new UserStats(summary, trend,
                byDifficulty(recent), byTag(recent));
    }

    private List<UserStats.DimensionStat> byDifficulty(List<Submission> recent) {
        Map<Long, Problem> problems = loadProblems(recent);
        Map<Integer, StatAccumulator> grouped = new HashMap<>();
        for (Submission item : recent) {
            Problem problem = problems.get(item.getProblemId());
            if (problem == null) {
                continue;
            }
            int difficulty = problem.getDifficulty() == null ? 1 : problem.getDifficulty();
            grouped.computeIfAbsent(difficulty, key -> new StatAccumulator()).add(item);
        }
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toStat(
                        String.valueOf(entry.getKey()),
                        difficultyLabel(entry.getKey()),
                        entry.getValue()))
                .toList();
    }

    private List<UserStats.DimensionStat> byTag(List<Submission> recent) {
        Map<Long, Problem> problems = loadProblems(recent);
        Map<String, StatAccumulator> grouped = new HashMap<>();
        for (Submission item : recent) {
            Problem problem = problems.get(item.getProblemId());
            if (problem == null) {
                continue;
            }
            for (String tag : splitTags(problem.getTags())) {
                grouped.computeIfAbsent(tag, key -> new StatAccumulator()).add(item);
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> toStat(entry.getKey(), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(UserStats.DimensionStat::solvedProblems)
                        .reversed()
                        .thenComparing(Comparator.comparingLong(
                                UserStats.DimensionStat::submissions).reversed()))
                .toList();
    }

    private Map<Long, Problem> loadProblems(List<Submission> recent) {
        List<Long> ids = recent.stream().map(Submission::getProblemId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, problem -> problem, (a, b) -> a));
    }

    private static UserStats.DimensionStat toStat(String key, String label,
                                                  StatAccumulator accumulator) {
        return new UserStats.DimensionStat(
                key, label, accumulator.submissions, accumulator.accepted,
                accumulator.solvedProblems.size());
    }

    private static String difficultyLabel(int difficulty) {
        return switch (difficulty) {
            case 3 -> "困难";
            case 2 -> "中等";
            default -> "简单";
        };
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
    }

    private static final class StatAccumulator {
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