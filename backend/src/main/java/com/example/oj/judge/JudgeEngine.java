package com.example.oj.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.config.JudgeProperties;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.entity.SubmissionCaseResult;
import com.example.oj.entity.TestCase;
import com.example.oj.judge.sandbox.SandboxResult;
import com.example.oj.judge.sandbox.SandboxRunner;
import com.example.oj.judge.sandbox.SandboxSpec;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionCaseResultMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.TestCaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class JudgeEngine {

    private static final Logger log = LoggerFactory.getLogger(JudgeEngine.class);

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final SubmissionCaseResultMapper caseResultMapper;
    private final SandboxRunner sandboxRunner;
    private final OutputComparator comparator;
    private final JudgeProperties properties;

    public JudgeEngine(SubmissionMapper submissionMapper,
                       ProblemMapper problemMapper,
                       TestCaseMapper testCaseMapper,
                       SubmissionCaseResultMapper caseResultMapper,
                       SandboxRunner sandboxRunner,
                       OutputComparator comparator,
                       JudgeProperties properties) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
        this.caseResultMapper = caseResultMapper;
        this.sandboxRunner = sandboxRunner;
        this.comparator = comparator;
        this.properties = properties;
    }

    public void judge(JudgeTask task) {
        try {
            judgeInternal(task);
        } catch (Exception ex) {
            log.error("judge failed, submissionId={}", task.submissionId(), ex);
            finish(task.submissionId(), "RE", 0, null, null, null, truncate(ex.getMessage()));
        }
    }

    private void judgeInternal(JudgeTask task) {
        if (!transition(task.submissionId(), "PENDING", "COMPILING")) {
            log.warn("submission {} is not pending, skip", task.submissionId());
            return;
        }

        Problem problem = problemMapper.selectById(task.problemId());
        if (problem == null) {
            finish(task.submissionId(), "RE", 0, null, null, null, "题目不存在");
            return;
        }

        List<TestCase> cases = testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, task.problemId())
                .orderByAsc(TestCase::getOrderNo));
        if (cases.isEmpty()) {
            finish(task.submissionId(), "RE", 0, null, null, null, "题目未配置测试点");
            return;
        }

        transition(task.submissionId(), "COMPILING", "RUNNING");
        int timeLimit = effectiveTimeLimit(problem, task.language());

        String finalStatus = "AC";
        int totalScore = 0;
        int maxTime = 0;
        int maxMemory = 0;
        String compileOutput = null;

        for (TestCase testCase : cases) {
            SandboxResult result = sandboxRunner.run(new SandboxSpec(
                    task.language(),
                    task.code(),
                    testCase.getInput(),
                    timeLimit,
                    problem.getMemoryLimitMb()));

            String caseStatus = judgeCase(result, testCase.getExpectedOutput());

            if (result.compileFailed()) {
                compileOutput = truncate(result.compileOutput());
            }
            maxTime = Math.max(maxTime, (int) result.elapsedMs());
            maxMemory = Math.max(maxMemory, (int) result.peakMemoryKb());

            saveCaseResult(task.submissionId(), testCase.getId(), caseStatus, result, testCase.getExpectedOutput());

            if ("AC".equals(caseStatus)) {
                totalScore += testCase.getScore() == null ? 0 : testCase.getScore();
            } else {
                finalStatus = caseStatus;
                break;
            }
        }

        finish(task.submissionId(), finalStatus, totalScore, maxTime, maxMemory, compileOutput, null);
    }

    private String judgeCase(SandboxResult result, String expectedOutput) {
        if (result.compileFailed()) {
            return "CE";
        }
        if (result.timedOut()) {
            return "TLE";
        }
        if (result.oomKilled()) {
            return "MLE";
        }
        if (result.exitCode() != 0) {
            return "RE";
        }
        return comparator.matches(expectedOutput, result.stdout()) ? "AC" : "WA";
    }

    private int effectiveTimeLimit(Problem problem, String language) {
        double multiplier = properties.getTimeMultiplier().getOrDefault(language, 1.0);
        return (int) Math.round(problem.getTimeLimitMs() * multiplier);
    }

    private boolean transition(Long submissionId, String from, String to) {
        int rows = submissionMapper.update(null, new LambdaUpdateWrapper<Submission>()
                .eq(Submission::getId, submissionId)
                .eq(Submission::getStatus, from)
                .set(Submission::getStatus, to));
        return rows > 0;
    }

    private void finish(Long submissionId, String status, int score,
                        Integer execTimeMs, Integer memoryKb,
                        String compileOutput, String errorMessage) {
        submissionMapper.update(null, new LambdaUpdateWrapper<Submission>()
                .eq(Submission::getId, submissionId)
                .in(Submission::getStatus, List.of("COMPILING", "RUNNING"))
                .set(Submission::getStatus, status)
                .set(Submission::getScore, score)
                .set(Submission::getExecTimeMs, execTimeMs)
                .set(Submission::getMemoryKb, memoryKb)
                .set(Submission::getCompileOutput, compileOutput)
                .set(Submission::getErrorMessage, errorMessage)
                .set(Submission::getFinishedAt, LocalDateTime.now()));
    }

    private void saveCaseResult(Long submissionId, Long caseId, String status,
                                SandboxResult result, String expectedOutput) {
        SubmissionCaseResult record = new SubmissionCaseResult();
        record.setSubmissionId(submissionId);
        record.setCaseId(caseId);
        record.setStatus(status);
        record.setExecTimeMs((int) result.elapsedMs());
        record.setMemoryKb((int) result.peakMemoryKb());
        record.setMessage("AC".equals(status) ? null : shapeMessage(expectedOutput, result.stdout()));
        try {
            caseResultMapper.insert(record);
        } catch (Exception ex) {
            log.warn("duplicate case result, submissionId={}, caseId={}", submissionId, caseId);
        }
    }

    private String shapeMessage(String expectedOutput, String actualOutput) {
        String expected = comparator.normalize(expectedOutput);
        String actual = comparator.normalize(actualOutput);
        return "act=" + countLines(actual) + ",chars=" + actual.length()
                + ";exp=" + countLines(expected) + ",chars=" + expected.length();
    }

    private int countLines(String text) {
        return text.isEmpty() ? 0 : text.split("\n", -1).length;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        int limit = properties.getCompileOutputLimitBytes();
        return text.length() <= limit ? text : text.substring(0, limit) + "\n...[truncated]";
    }
}