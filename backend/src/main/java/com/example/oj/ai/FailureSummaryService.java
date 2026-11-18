package com.example.oj.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.entity.Submission;
import com.example.oj.entity.SubmissionCaseResult;
import com.example.oj.entity.TestCase;
import com.example.oj.mapper.SubmissionCaseResultMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.TestCaseMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FailureSummaryService {

    private static final Pattern SHAPE = Pattern.compile("act=(\\d+),chars=(\\d+);exp=(\\d+),chars=(\\d+)");

    private final SubmissionMapper submissionMapper;
    private final TestCaseMapper testCaseMapper;
    private final SubmissionCaseResultMapper caseResultMapper;

    public FailureSummaryService(SubmissionMapper submissionMapper,
                                 TestCaseMapper testCaseMapper,
                                 SubmissionCaseResultMapper caseResultMapper) {
        this.submissionMapper = submissionMapper;
        this.testCaseMapper = testCaseMapper;
        this.caseResultMapper = caseResultMapper;
    }

    public FailureSummary summarize(Long submissionId, Long userId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || !submission.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询自己的提交");
        }

        List<SubmissionCaseResult> results = caseResultMapper.selectList(
                new LambdaQueryWrapper<SubmissionCaseResult>()
                        .eq(SubmissionCaseResult::getSubmissionId, submissionId));
        Map<Long, Integer> orderNoMap = testCaseMapper.selectList(
                        new LambdaQueryWrapper<TestCase>()
                                .eq(TestCase::getProblemId, submission.getProblemId()))
                .stream()
                .collect(Collectors.toMap(TestCase::getId, TestCase::getOrderNo));

        int passed = (int) results.stream().filter(item -> "AC".equals(item.getStatus())).count();
        SubmissionCaseResult firstFailed = results.stream()
                .filter(item -> !"AC".equals(item.getStatus()))
                .min((a, b) -> Integer.compare(
                        orderNoMap.getOrDefault(a.getCaseId(), 0),
                        orderNoMap.getOrDefault(b.getCaseId(), 0)))
                .orElse(null);

        Integer orderNo = null;
        String failedStatus = null;
        Integer actualLines = null;
        Integer actualChars = null;
        Integer expectedLines = null;
        Integer expectedChars = null;
        Integer execTimeMs = null;
        Integer memoryKb = null;

        if (firstFailed != null) {
            orderNo = orderNoMap.getOrDefault(firstFailed.getCaseId(), 0);
            failedStatus = firstFailed.getStatus();
            execTimeMs = firstFailed.getExecTimeMs();
            memoryKb = firstFailed.getMemoryKb();
            Matcher matcher = SHAPE.matcher(firstFailed.getMessage() == null ? "" : firstFailed.getMessage());
            if (matcher.find()) {
                actualLines = Integer.parseInt(matcher.group(1));
                actualChars = Integer.parseInt(matcher.group(2));
                expectedLines = Integer.parseInt(matcher.group(3));
                expectedChars = Integer.parseInt(matcher.group(4));
            }
        }

        return new FailureSummary(
                results.size(), passed, orderNo, failedStatus,
                actualLines, actualChars, expectedLines, expectedChars,
                execTimeMs, memoryKb);
    }

    public record FailureSummary(
            int totalCases,
            int passedCases,
            Integer firstFailedOrderNo,
            String firstFailedStatus,
            Integer actualLines,
            Integer actualChars,
            Integer expectedLines,
            Integer expectedChars,
            Integer execTimeMs,
            Integer memoryKb) {
    }
}