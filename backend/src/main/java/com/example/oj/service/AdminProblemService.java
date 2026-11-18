package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.ProblemUpsertRequest;
import com.example.oj.dto.TestCaseBatchRequest;
import com.example.oj.entity.Problem;
import com.example.oj.entity.TestCase;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.TestCaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

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
    }

    private String writeSamples(java.util.List<ProblemUpsertRequest.Sample> samples) {
        if (samples == null || samples.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(samples);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}