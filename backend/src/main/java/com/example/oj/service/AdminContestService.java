package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.ContestDetail;
import com.example.oj.dto.ContestListItem;
import com.example.oj.dto.ContestProblemAssignRequest;
import com.example.oj.dto.ContestProblemItem;
import com.example.oj.dto.ContestStandings;
import com.example.oj.dto.ContestUpsertRequest;
import com.example.oj.dto.PageResponse;
import com.example.oj.entity.Contest;
import com.example.oj.entity.ContestParticipant;
import com.example.oj.entity.ContestProblem;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.ContestMapper;
import com.example.oj.mapper.ContestParticipantMapper;
import com.example.oj.mapper.ContestProblemMapper;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminContestService {

    private static final List<String> MODES = List.of("ICPC", "OI");
    private static final List<String> JOIN_TYPES = List.of("PUBLIC", "PASSWORD");

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestParticipantMapper participantMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final ContestService contestService;

    public AdminContestService(ContestMapper contestMapper,
                               ContestProblemMapper contestProblemMapper,
                               ContestParticipantMapper participantMapper,
                               ProblemMapper problemMapper,
                               SubmissionMapper submissionMapper,
                               PasswordEncoder passwordEncoder,
                               ContestService contestService) {
        this.contestMapper = contestMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.participantMapper = participantMapper;
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.contestService = contestService;
    }

    public PageResponse<ContestListItem> list(int pageNo, int size, String keyword) {
        Page<Contest> page = new Page<>(pageNo, Math.min(size, 100));
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<Contest>()
                .like(keyword != null && !keyword.isBlank(), Contest::getTitle, keyword)
                .orderByDesc(Contest::getStartAt);
        Page<Contest> result = contestMapper.selectPage(page, wrapper);
        LocalDateTime now = LocalDateTime.now();
        List<ContestListItem> records = result.getRecords().stream()
                .map(contest -> contestService.toAdminListItem(contest, now))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    public ContestDetail detail(Long contestId) {
        Contest contest = requireContest(contestId);
        List<ContestProblem> mappings = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId)
                        .orderByAsc(ContestProblem::getOrderNo));
        List<Long> problemIds = mappings.stream().map(ContestProblem::getProblemId).distinct().toList();
        Map<Long, Problem> problemMap = problemIds.isEmpty()
                ? Map.of()
                : problemMapper.selectBatchIds(problemIds).stream()
                        .collect(Collectors.toMap(Problem::getId, Function.identity()));
        List<ContestProblemItem> problems = mappings.stream()
                .map(mapping -> {
                    Problem problem = problemMap.get(mapping.getProblemId());
                    if (problem == null) {
                        return null;
                    }
                    return new ContestProblemItem(
                            problem.getId(),
                            mapping.getAlias(),
                            mapping.getOrderNo(),
                            problem.getTitle(),
                            problem.getDifficulty(),
                            false,
                            mapping.getScore());
                })
                .filter(item -> item != null)
                .toList();
        return new ContestDetail(
                contest.getId(),
                contest.getTitle(),
                contest.getDescription(),
                contest.getMode(),
                ContestService.phase(contest, LocalDateTime.now()),
                contest.getStartAt(),
                contest.getEndAt(),
                contest.getFreezeAt(),
                contest.getJoinType(),
                contest.getPublished() != null && contest.getPublished() == 1,
                false,
                false,
                contestService.participantCount(contestId),
                contestService.problemCount(contestId),
                problems);
    }

    @Transactional
    public Long create(ContestUpsertRequest request, Long adminId) {
        validate(request, true);
        Contest contest = new Contest();
        apply(contest, request);
        contest.setCreatedBy(adminId);
        contest.setPublished(Boolean.TRUE.equals(request.published()) ? 1 : 0);
        if ("PASSWORD".equals(request.joinType())) {
            contest.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        contestMapper.insert(contest);
        return contest.getId();
    }

    @Transactional
    public void update(Long contestId, ContestUpsertRequest request) {
        Contest contest = requireContest(contestId);
        validate(request, false);
        apply(contest, request);
        if ("PASSWORD".equals(request.joinType())) {
            if (request.password() != null && !request.password().isBlank()) {
                contest.setPasswordHash(passwordEncoder.encode(request.password()));
            }
        } else {
            contest.setPasswordHash(null);
        }
        if (request.published() != null) {
            contest.setPublished(request.published() ? 1 : 0);
        }
        contestMapper.updateById(contest);
    }

    @Transactional
    public void delete(Long contestId) {
        requireContest(contestId);
        participantMapper.delete(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId));
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        submissionMapper.update(null, new LambdaUpdateWrapper<Submission>()
                .eq(Submission::getContestId, contestId)
                .set(Submission::getContestId, null));
        contestMapper.deleteById(contestId);
    }

    @Transactional
    public void assignProblems(Long contestId, ContestProblemAssignRequest request) {
        Contest contest = requireContest(contestId);
        boolean oi = "OI".equals(contest.getMode());
        List<ContestProblemAssignRequest.Item> items = request.problems();
        Set<Long> problemIds = items.stream().map(ContestProblemAssignRequest.Item::problemId).collect(Collectors.toSet());
        if (problemIds.size() != items.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "比赛题目不能重复");
        }
        List<Problem> problems = problemMapper.selectBatchIds(problemIds);
        if (problems.size() != problemIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在不存在的题目");
        }

        Set<String> aliases = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        List<ContestProblem> mappings = new java.util.ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            ContestProblemAssignRequest.Item item = items.get(index);
            String alias = item.alias() == null || item.alias().isBlank()
                    ? aliasOf(index) : item.alias().trim();
            int orderNo = item.orderNo() == null || item.orderNo() <= 0 ? index + 1 : item.orderNo();
            if (!aliases.add(alias)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "题目编号不能重复：" + alias);
            }
            if (!orders.add(orderNo)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "题目顺序不能重复：" + orderNo);
            }
            ContestProblem mapping = new ContestProblem();
            mapping.setContestId(contestId);
            mapping.setProblemId(item.problemId());
            mapping.setAlias(alias);
            mapping.setOrderNo(orderNo);
            if (oi && (item.score() == null || item.score() <= 0)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "OI 赛制下每道题都必须设置大于 0 的分值");
            }
            mapping.setScore(oi ? item.score() : null);
            mappings.add(mapping);
        }

        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        for (ContestProblem mapping : mappings) {
            contestProblemMapper.insert(mapping);
        }
    }

    public void registerParticipant(Long contestId, Long userId) {
        requireContest(contestId);
        if (contestService.isRegistered(contestId, userId)) {
            return;
        }
        ContestParticipant participant = new ContestParticipant();
        participant.setContestId(contestId);
        participant.setUserId(userId);
        participant.setRegisteredAt(LocalDateTime.now());
        participantMapper.insert(participant);
    }

    public ContestStandings adminStandings(Long contestId) {
        return contestService.adminStandings(contestId);
    }

    private void validate(ContestUpsertRequest request, boolean creating) {
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
        if (request.freezeAt() != null
                && (request.freezeAt().isBefore(request.startAt()) || request.freezeAt().isAfter(request.endAt()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "封榜时间必须在比赛时间范围内");
        }
        if (!MODES.contains(request.mode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的赛制");
        }
        if (!JOIN_TYPES.contains(request.joinType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的报名方式");
        }
        if ("PASSWORD".equals(request.joinType())
                && creating
                && (request.password() == null || request.password().isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码报名必须设置密码");
        }
    }

    private void apply(Contest contest, ContestUpsertRequest request) {
        contest.setTitle(request.title().trim());
        contest.setDescription(request.description());
        contest.setMode(request.mode());
        contest.setStartAt(request.startAt());
        contest.setEndAt(request.endAt());
        contest.setFreezeAt(request.freezeAt());
        contest.setJoinType(request.joinType());
    }

    private Contest requireContest(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "比赛不存在");
        }
        return contest;
    }

    private String aliasOf(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "P" + (index + 1);
    }
}