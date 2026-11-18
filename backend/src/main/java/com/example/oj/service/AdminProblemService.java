package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.AdminProblemDetail;
import com.example.oj.dto.AdminProblemItem;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.ProblemUpsertRequest;
import com.example.oj.dto.TestCaseBatchRequest;
import com.example.oj.entity.Problem;
import com.example.oj.entity.TestCase;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.TestCaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminProblemService {

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final ObjectMapper objectMapper;

    public AdminProblemService(ProblemMapper problemMapper,
                               TestCaseMapper testCaseMapper,
                               ObjectMapper objectMapper) {
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
        this.objectMapper = objectMapper;
    }

    public PageResponse<AdminProblemItem> list(int pageNo, int size, String keyword, Integer difficulty) {
        Page<Problem> page = new Page<>(pageNo, Math.min(size, 100));
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Problem::getTitle, keyword.trim());
        }
        wrapper.eq(difficulty != null, Problem::getDifficulty, difficulty);
        wrapper.orderByAsc(Problem::getId);

        Page<Problem> result = problemMapper.selectPage(page, wrapper);
        List<Long> ids = result.getRecords().stream().map(Problem::getId).toList();
        Map<Long, Long> caseCounts = ids.isEmpty() ? Map.of() : testCaseMapper.selectList(
                        new LambdaQueryWrapper<TestCase>().in(TestCase::getProblemId, ids))
                .stream()
                .collect(Collectors.groupingBy(TestCase::getProblemId, Collectors.counting()));

        List<AdminProblemItem> records = result.getRecords().stream()
                .map(problem -> new AdminProblemItem(
                        problem.getId(),
                        problem.getTitle(),
                        problem.getDifficulty(),
                        problem.getTags(),
                        problem.getVisible(),
                        problem.getTimeLimitMs(),
                        problem.getMemoryLimitMb(),
                        caseCounts.getOrDefault(problem.getId(), 0L).intValue(),
                        problem.getCreatedAt()))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    public AdminProblemDetail detail(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<AdminProblemDetail.CaseView> cases = testCaseMapper.selectList(
                        new LambdaQueryWrapper<TestCase>()
                                .eq(TestCase::getProblemId, problemId)
                                .orderByAsc(TestCase::getOrderNo))
                .stream()
                .map(item -> new AdminProblemDetail.CaseView(
                        item.getOrderNo(), item.getInput(), item.getExpectedOutput(), item.getScore()))
                .toList();
        return new AdminProblemDetail(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                readSamples(problem.getSamples()),
                problem.getDifficulty(),
                problem.getTags(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                problem.getVisible(),
                cases);
    }

    public Long create(ProblemUpsertRequest request, Long adminId) {
        Problem problem = new Problem();
        apply(problem, request);
        problem.setCreatedBy(adminId);
        problemMapper.insert(problem);
        return problem.getId();
    }

    public void update(Long problemId, ProblemUpsertRequest request) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        apply(problem, request);
        problemMapper.updateById(problem);
    }

    @Transactional
    public void replaceTestCases(Long problemId, TestCaseBatchRequest request) {
        if (problemMapper.selectById(problemId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Set<Integer> seen = new HashSet<>();
        for (TestCaseBatchRequest.Item item : request.testCases()) {
            if (!seen.add(item.orderNo())) {
                throw new BusinessException(ErrorCode.DUPLICATE_CASE_ORDER, "序号重复：" + item.orderNo());
            }
        }

        testCaseMapper.delete(new LambdaQueryWrapper<TestCase>().eq(TestCase::getProblemId, problemId));
        for (TestCaseBatchRequest.Item item : request.testCases()) {
            TestCase testCase = new TestCase();
            testCase.setProblemId(problemId);
            testCase.setOrderNo(item.orderNo());
            testCase.setInput(item.input());
            testCase.setExpectedOutput(item.expectedOutput());
            testCase.setScore(item.score() == null ? 0 : item.score());
            testCaseMapper.insert(testCase);
        }
    }

    private void apply(Problem problem, ProblemUpsertRequest request) {
        problem.setTitle(request.title());
        problem.setDescription(request.description());
        problem.setSamples(writeSamples(request.samples()));
        problem.setDifficulty(request.difficulty() == null ? 1 : request.difficulty());
        problem.setTimeLimitMs(request.timeLimitMs() == null ? 1000 : request.timeLimitMs());
        problem.setMemoryLimitMb(request.memoryLimitMb() == null ? 256 : request.memoryLimitMb());
        problem.setVisible(request.visible() == null ? 1 : request.visible());
        problem.setTags(normalizeTags(request.tags()));
    }

    private static String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return "";
        }
        String normalized = Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        return normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
    }

    private String writeSamples(List<ProblemUpsertRequest.Sample> samples) {
        if (samples == null || samples.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(samples);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<ProblemUpsertRequest.Sample> readSamples(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<ProblemUpsertRequest.Sample>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}