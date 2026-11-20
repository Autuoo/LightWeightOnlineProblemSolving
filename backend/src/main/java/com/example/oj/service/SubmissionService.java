package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.JudgeProperties;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.SubmissionDetail;
import com.example.oj.dto.SubmissionListItem;
import com.example.oj.dto.SubmitRequest;
import com.example.oj.dto.SubmitResponse;
import com.example.oj.entity.Contest;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.entity.SubmissionCaseResult;
import com.example.oj.entity.TestCase;
import com.example.oj.judge.JudgeDispatcher;
import com.example.oj.judge.JudgeTask;
import com.example.oj.judge.SuspiciousCodeDetector;
import com.example.oj.mapper.ContestMapper;
import com.example.oj.mapper.ContestParticipantMapper;
import com.example.oj.mapper.ContestProblemMapper;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionCaseResultMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.TestCaseMapper;
import com.example.oj.util.HashUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private static final List<String> LANGUAGES = List.of("PYTHON3", "CPP17", "JAVA17");
    private static final List<String> TERMINAL_STATUSES =
            List.of("AC", "WA", "TLE", "MLE", "RE", "CE", "REJECTED");

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final SubmissionCaseResultMapper caseResultMapper;
    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestParticipantMapper contestParticipantMapper;
    private final ProblemService problemService;
    private final JudgeDispatcher judgeDispatcher;
    private final JudgeProperties judgeProperties;
    private final SuspiciousCodeDetector suspiciousCodeDetector;

    public SubmissionService(SubmissionMapper submissionMapper,
                             ProblemMapper problemMapper,
                             TestCaseMapper testCaseMapper,
                             SubmissionCaseResultMapper caseResultMapper,
                             ContestMapper contestMapper,
                             ContestProblemMapper contestProblemMapper,
                             ContestParticipantMapper contestParticipantMapper,
                             ProblemService problemService,
                             JudgeDispatcher judgeDispatcher,
                             JudgeProperties judgeProperties,
                             SuspiciousCodeDetector suspiciousCodeDetector) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
        this.caseResultMapper = caseResultMapper;
        this.contestMapper = contestMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.contestParticipantMapper = contestParticipantMapper;
        this.problemService = problemService;
        this.judgeDispatcher = judgeDispatcher;
        this.judgeProperties = judgeProperties;
        this.suspiciousCodeDetector = suspiciousCodeDetector;
    }

    public SubmitResponse submit(Long userId, SubmitRequest request) {
        if (!LANGUAGES.contains(request.language())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_LANGUAGE);
        }

        Long contestId = request.contestId();
        Problem problem;
        if (contestId != null) {
            Contest contest = contestMapper.selectById(contestId);
            if (contest == null || contest.getPublished() == null || contest.getPublished() == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "比赛不存在或未发布");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(contest.getStartAt()) || !now.isBefore(contest.getEndAt())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "比赛未开始或已结束");
            }
            long registered = contestParticipantMapper.selectCount(
                    new LambdaQueryWrapper<com.example.oj.entity.ContestParticipant>()
                            .eq(com.example.oj.entity.ContestParticipant::getContestId, contestId)
                            .eq(com.example.oj.entity.ContestParticipant::getUserId, userId));
            if (registered == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "请先报名比赛");
            }
            long mapped = contestProblemMapper.selectCount(
                    new LambdaQueryWrapper<com.example.oj.entity.ContestProblem>()
                            .eq(com.example.oj.entity.ContestProblem::getContestId, contestId)
                            .eq(com.example.oj.entity.ContestProblem::getProblemId, request.problemId()));
            if (mapped == 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "题目不属于该比赛");
            }
            problem = problemMapper.selectById(request.problemId());
            if (problem == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
            }
        } else {
            problem = problemService.requireVisible(request.problemId());
        }

        String codeHash = HashUtils.sha256(request.language() + "\n" + request.code());
        // 比赛提交不复用幂等结果，保证每次尝试都被记录并计入罚时。
        if (contestId == null) {
            Submission existing = findIdempotent(userId, request.problemId(), codeHash);
            if (existing != null) {
                return new SubmitResponse(existing.getId(), existing.getStatus(), true);
            }
        }

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problem.getId());
        submission.setContestId(contestId);
        submission.setLanguage(request.language());
        submission.setCode(request.code());
        submission.setCodeHash(codeHash);
        submission.setStatus("PENDING");
        submission.setScore(0);
        SuspiciousCodeDetector.Result detection =
                suspiciousCodeDetector.detect(request.language(), request.code());
        submission.setSuspicious(detection.suspicious() ? 1 : 0);
        submission.setSuspiciousReason(detection.reason());
        submissionMapper.insert(submission);

        boolean accepted = judgeDispatcher.dispatch(
                new JudgeTask(submission.getId(), problem.getId(), request.language(), request.code()));
        if (!accepted) {
            submissionMapper.update(null, new LambdaUpdateWrapper<Submission>()
                    .eq(Submission::getId, submission.getId())
                    .eq(Submission::getStatus, "PENDING")
                    .set(Submission::getStatus, "REJECTED"));
            throw new BusinessException(ErrorCode.JUDGE_QUEUE_FULL);
        }
        return new SubmitResponse(submission.getId(), "PENDING", false);
    }

    public SubmissionDetail detail(Long userId, boolean admin, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!admin && !submission.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Problem problem = problemMapper.selectById(submission.getProblemId());
        String problemTitle = problem == null ? "" : problem.getTitle();

        String contestMode = null;
        if (submission.getContestId() != null) {
            Contest contest = contestMapper.selectById(submission.getContestId());
            contestMode = contest == null ? null : contest.getMode();
        }

        List<SubmissionCaseResult> caseResults = caseResultMapper.selectList(
                new LambdaQueryWrapper<SubmissionCaseResult>()
                        .eq(SubmissionCaseResult::getSubmissionId, submissionId)
                        .orderByAsc(SubmissionCaseResult::getCaseId));

        Map<Long, Integer> orderNoMap = testCaseMapper.selectList(
                        new LambdaQueryWrapper<TestCase>()
                                .eq(TestCase::getProblemId, submission.getProblemId()))
                .stream()
                .collect(Collectors.toMap(TestCase::getId, TestCase::getOrderNo));

        List<SubmissionDetail.CaseView> cases = caseResults.stream()
                .map(item -> new SubmissionDetail.CaseView(
                        item.getCaseId(),
                        orderNoMap.getOrDefault(item.getCaseId(), 0),
                        item.getStatus(),
                        item.getExecTimeMs(),
                        item.getMemoryKb()))
                .toList();

        return new SubmissionDetail(
                submission.getId(),
                submission.getProblemId(),
                submission.getContestId(),
                contestMode,
                problemTitle,
                submission.getLanguage(),
                submission.getCode(),
                submission.getStatus(),
                submission.getScore(),
                submission.getExecTimeMs(),
                submission.getMemoryKb(),
                submission.getCompileOutput(),
                submission.getErrorMessage(),
                submission.getSuspicious() != null && submission.getSuspicious() == 1,
                submission.getSuspiciousReason(),
                submission.getCreatedAt(),
                submission.getFinishedAt(),
                submission.getUserId().equals(userId),
                cases);
    }

    public PageResponse<SubmissionListItem> list(Long userId, int pageNo, int size, Long problemId,
                                                 Long contestId, String status, String keyword,
                                                 LocalDateTime from, LocalDateTime to,
                                                 Boolean suspicious) {
        Page<Submission> page = new Page<>(pageNo, Math.min(size, 100));
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(problemId != null, Submission::getProblemId, problemId)
                .eq(contestId != null, Submission::getContestId, contestId)
                .eq(status != null && !status.isBlank(), Submission::getStatus, status)
                .eq(suspicious != null, Submission::getSuspicious, Boolean.TRUE.equals(suspicious) ? 1 : 0)
                .ge(from != null, Submission::getCreatedAt, from)
                .lt(to != null, Submission::getCreatedAt, to)
                .orderByDesc(Submission::getCreatedAt);

        if (keyword != null && !keyword.isBlank()) {
            List<Long> problemIds = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                            .like(Problem::getTitle, keyword.trim()))
                    .stream().map(Problem::getId).toList();
            if (problemIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageNo, size, 0);
            }
            wrapper.in(Submission::getProblemId, problemIds);
        }

        Page<Submission> result = submissionMapper.selectPage(page, wrapper);

        Map<Long, String> titleMap = problemTitles(result.getRecords());
        List<SubmissionListItem> records = result.getRecords().stream()
                .map(item -> new SubmissionListItem(
                        item.getId(),
                        item.getProblemId(),
                        titleMap.getOrDefault(item.getProblemId(), ""),
                        item.getLanguage(),
                        item.getStatus(),
                        item.getScore(),
                        item.getExecTimeMs(),
                        item.getMemoryKb(),
                        item.getCreatedAt(),
                        item.getFinishedAt(),
                        item.getSuspicious() != null && item.getSuspicious() == 1))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    private Submission findIdempotent(Long userId, Long problemId, String codeHash) {
        LocalDateTime windowStart = LocalDateTime.now()
                .minusMinutes(judgeProperties.getIdempotentWindowMinutes());
        return submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(Submission::getProblemId, problemId)
                .eq(Submission::getCodeHash, codeHash)
                .ge(Submission::getCreatedAt, windowStart)
                .in(Submission::getStatus, TERMINAL_STATUSES)
                .orderByDesc(Submission::getCreatedAt)
                .last("limit 1"));
    }

    private Map<Long, String> problemTitles(List<Submission> submissions) {
        List<Long> ids = submissions.stream().map(Submission::getProblemId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle, (a, b) -> a));
    }
}