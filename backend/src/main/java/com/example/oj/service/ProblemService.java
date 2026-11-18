package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.JudgeProperties;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.ProblemDetail;
import com.example.oj.dto.ProblemListItem;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private static final List<String> LANGUAGES = List.of("PYTHON3", "CPP17", "JAVA17");

    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final JudgeProperties judgeProperties;
    private final ObjectMapper objectMapper;

    public ProblemService(ProblemMapper problemMapper,
                          SubmissionMapper submissionMapper,
                          JudgeProperties judgeProperties,
                          ObjectMapper objectMapper) {
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
        this.judgeProperties = judgeProperties;
        this.objectMapper = objectMapper;
    }

    public PageResponse<ProblemListItem> list(Long userId, int pageNo, int size, String keyword, Integer difficulty) {
        Page<Problem> page = new Page<>(pageNo, Math.min(size, 100));
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getVisible, 1)
                .like(keyword != null && !keyword.isBlank(), Problem::getTitle, keyword)
                .eq(difficulty != null, Problem::getDifficulty, difficulty)
                .orderByDesc(Problem::getCreatedAt);
        Page<Problem> result = problemMapper.selectPage(page, wrapper);

        Set<Long> passed = passedProblemIds(userId);
        List<Long> problemIds = result.getRecords().stream().map(Problem::getId).toList();
        Map<Long, int[]> stats = problemStats(problemIds);
        List<ProblemListItem> records = result.getRecords().stream()
                .map(problem -> {
                    int[] stat = stats.getOrDefault(problem.getId(), new int[]{0, 0});
                    double rate = stat[0] == 0 ? 0.0 : (double) stat[1] / stat[0];
                    return new ProblemListItem(
                            problem.getId(),
                            problem.getTitle(),
                            problem.getDifficulty(),
                            problem.getTimeLimitMs(),
                            problem.getMemoryLimitMb(),
                            passed.contains(problem.getId()),
                            stat[0],
                            stat[1],
                            Math.round(rate * 10000) / 10000.0);
                })
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    private Map<Long, int[]> problemStats(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Submission> query = new QueryWrapper<>();
        query.select(
                "problem_id AS problemId",
                "COUNT(*) AS submitCount",
                "SUM(CASE WHEN status = 'AC' THEN 1 ELSE 0 END) AS acceptedCount");
        query.in("problem_id", problemIds);
        query.groupBy("problem_id");

        Map<Long, int[]> stats = new HashMap<>();
        for (Map<String, Object> row : submissionMapper.selectMaps(query)) {
            Long problemId = toLong(row.get("problemId"));
            if (problemId != null) {
                stats.put(problemId, new int[]{toInt(row.get("submitCount")), toInt(row.get("acceptedCount"))});
            }
        }
        return stats;
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public ProblemDetail detail(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || problem.getVisible() == null || problem.getVisible() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return new ProblemDetail(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                problem.getDifficulty(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                LANGUAGES,
                judgeProperties.getTimeMultiplier(),
                parseSamples(problem.getSamples()));
    }

    public Problem requireVisible(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || problem.getVisible() == null || problem.getVisible() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在或不可见");
        }
        return problem;
    }

    private Set<Long> passedProblemIds(Long userId) {
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(Submission::getStatus, "AC")
                .select(Submission::getProblemId));
        return submissions.stream()
                .map(Submission::getProblemId)
                .collect(Collectors.toSet());
    }

    private JsonNode parseSamples(String samples) {
        if (samples == null || samples.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            return objectMapper.readTree(samples);
        } catch (Exception ex) {
            return objectMapper.createArrayNode();
        }
    }
}