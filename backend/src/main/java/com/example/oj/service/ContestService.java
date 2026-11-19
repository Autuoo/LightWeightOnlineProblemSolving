package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.ContestDetail;
import com.example.oj.dto.ContestListItem;
import com.example.oj.dto.ContestProblemItem;
import com.example.oj.dto.ContestRegisterRequest;
import com.example.oj.dto.ContestStandingItem;
import com.example.oj.dto.ContestStandings;
import com.example.oj.entity.Contest;
import com.example.oj.entity.ContestParticipant;
import com.example.oj.entity.ContestProblem;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.entity.User;
import com.example.oj.mapper.ContestMapper;
import com.example.oj.mapper.ContestParticipantMapper;
import com.example.oj.mapper.ContestProblemMapper;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContestService {

    private static final List<String> COUNTED_STATUSES = List.of("AC", "WA", "TLE", "MLE", "RE", "CE");

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestParticipantMapper participantMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ProblemService problemService;

    public ContestService(ContestMapper contestMapper,
                          ContestProblemMapper contestProblemMapper,
                          ContestParticipantMapper participantMapper,
                          ProblemMapper problemMapper,
                          SubmissionMapper submissionMapper,
                          UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          ProblemService problemService) {
        this.contestMapper = contestMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.participantMapper = participantMapper;
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.problemService = problemService;
    }

    public static String phase(Contest contest, LocalDateTime now) {
        if (contest.getPublished() == null || contest.getPublished() == 0) {
            return "DRAFT";
        }
        if (now.isBefore(contest.getStartAt())) {
            return "UPCOMING";
        }
        if (!now.isBefore(contest.getEndAt())) {
            return "ENDED";
        }
        if (contest.getFreezeAt() != null && !now.isBefore(contest.getFreezeAt())) {
            return "FROZEN";
        }
        return "RUNNING";
    }

    public List<ContestListItem> list(Long userId, String keyword) {
        List<Contest> contests = contestMapper.selectList(new LambdaQueryWrapper<Contest>()
                .eq(Contest::getPublished, 1)
                .like(keyword != null && !keyword.isBlank(), Contest::getTitle, keyword)
                .orderByDesc(Contest::getStartAt));
        LocalDateTime now = LocalDateTime.now();
        return contests.stream()
                .map(contest -> toListItem(contest, userId, now))
                .toList();
    }

    public ContestDetail detail(Long contestId, Long userId) {
        Contest contest = requirePublished(contestId);
        LocalDateTime now = LocalDateTime.now();
        boolean registered = isRegistered(contestId, userId);
        List<ContestProblem> mappings = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId)
                        .orderByAsc(ContestProblem::getOrderNo));
        Map<Long, Problem> problemMap = problemMap(mappings);
        Set<Long> passed = passedProblemIds(contestId, userId);

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
                            passed.contains(problem.getId()),
                            mapping.getScore());
                })
                .filter(item -> item != null)
                .toList();

        String currentPhase = phase(contest, now);
        List<ContestProblemItem> visibleProblems =
                "UPCOMING".equals(currentPhase) ? List.of() : problems;
        return new ContestDetail(
                contest.getId(),
                contest.getTitle(),
                contest.getDescription(),
                contest.getMode(),
                currentPhase,
                contest.getStartAt(),
                contest.getEndAt(),
                contest.getFreezeAt(),
                contest.getJoinType(),
                true,
                registered,
                "UPCOMING".equals(currentPhase) && !registered,
                participantCount(contestId),
                problemCount(contestId),
                visibleProblems);
    }

    public void register(Long contestId, Long userId, ContestRegisterRequest request) {
        Contest contest = requirePublished(contestId);
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(contest.getStartAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "比赛已开始，无法报名");
        }
        if (isRegistered(contestId, userId)) {
            return;
        }
        String joinType = contest.getJoinType() == null ? "PUBLIC" : contest.getJoinType();
        if ("PASSWORD".equals(joinType)) {
            String password = request == null ? null : request.password();
            if (password == null || password.isBlank()
                    || contest.getPasswordHash() == null
                    || !passwordEncoder.matches(password, contest.getPasswordHash())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "报名密码错误");
            }
        } else if (!"PUBLIC".equals(joinType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该比赛不支持自助报名");
        }

        ContestParticipant participant = new ContestParticipant();
        participant.setContestId(contestId);
        participant.setUserId(userId);
        participant.setRegisteredAt(now);
        participantMapper.insert(participant);
    }

    public com.example.oj.dto.ProblemDetail problemDetail(Long contestId, Long problemId, Long userId) {
        Contest contest = requirePublished(contestId);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(contest.getStartAt())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "比赛尚未开始");
        }
        requireRegistered(contestId, userId);
        long mapped = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId));
        if (mapped == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不属于该比赛");
        }
        return problemService.detailAny(problemId);
    }

    public ContestStandings standings(Long contestId, Long userId) {
        Contest contest = requirePublished(contestId);
        requireRegistered(contestId, userId);
        return buildStandings(contest, false);
    }

    public ContestStandings adminStandings(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return buildStandings(contest, true);
    }

    private ContestStandings buildStandings(Contest contest, boolean admin) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = contest.getEndAt();
        boolean frozen = false;
        if (!admin && contest.getFreezeAt() != null
                && !now.isBefore(contest.getFreezeAt())
                && now.isBefore(contest.getEndAt())) {
            cutoff = contest.getFreezeAt();
            frozen = true;
        }

        List<ContestProblem> mappings = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contest.getId())
                        .orderByAsc(ContestProblem::getOrderNo));
        if (mappings.isEmpty()) {
            return new ContestStandings(contest.getId(), phase(contest, now), frozen,
                    contest.getFreezeAt(), now, List.of());
        }

        List<ContestParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<ContestParticipant>()
                        .eq(ContestParticipant::getContestId, contest.getId()));
        if (participants.isEmpty()) {
            return new ContestStandings(contest.getId(), phase(contest, now), frozen,
                    contest.getFreezeAt(), now, List.of());
        }

        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getContestId, contest.getId())
                .ge(Submission::getCreatedAt, contest.getStartAt())
                .lt(Submission::getCreatedAt, cutoff)
                .in(Submission::getStatus, COUNTED_STATUSES)
                .orderByAsc(Submission::getCreatedAt));

        Map<Long, Map<Long, Attempt>> attempts = new HashMap<>();
        for (Submission submission : submissions) {
            Attempt attempt = attempts
                    .computeIfAbsent(submission.getUserId(), key -> new HashMap<>())
                    .computeIfAbsent(submission.getProblemId(), key -> new Attempt());
            if ("AC".equals(submission.getStatus())) {
                if (attempt.firstAcAt == null) {
                    attempt.firstAcAt = submission.getCreatedAt();
                }
            } else {
                attempt.tries++;
                if (!"CE".equals(submission.getStatus()) && attempt.firstAcAt == null) {
                    attempt.wrong++;
                }
            }
        }

        List<Long> userIds = participants.stream().map(ContestParticipant::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ContestStandingItem> items = new ArrayList<>();
        for (ContestParticipant participant : participants) {
            User user = userMap.get(participant.getUserId());
            int solved = 0;
            int penalty = 0;
            LocalDateTime lastAcAt = null;
            Map<Long, Attempt> userAttempts = attempts.getOrDefault(participant.getUserId(), Map.of());
            LinkedHashMap<String, String> cells = new LinkedHashMap<>();
            for (ContestProblem mapping : mappings) {
                Attempt attempt = userAttempts.get(mapping.getProblemId());
                if (attempt != null && attempt.firstAcAt != null) {
                    solved++;
                    int minutes = (int) Duration.between(contest.getStartAt(), attempt.firstAcAt).toMinutes();
                    penalty += Math.max(0, minutes) + 20 * attempt.wrong;
                    if (lastAcAt == null || attempt.firstAcAt.isAfter(lastAcAt)) {
                        lastAcAt = attempt.firstAcAt;
                    }
                    cells.put(mapping.getAlias(), "+" + attempt.wrong);
                } else if (attempt != null && attempt.tries > 0) {
                    cells.put(mapping.getAlias(), "-" + attempt.tries);
                } else {
                    cells.put(mapping.getAlias(), "");
                }
            }
            items.add(new ContestStandingItem(
                    0,
                    participant.getUserId(),
                    user == null ? "-" : user.getUsername(),
                    user == null ? "-" : user.getNickname(),
                    solved,
                    penalty,
                    lastAcAt,
                    cells));
        }

        items.sort(Comparator
                .comparingInt(ContestStandingItem::solved).reversed()
                .thenComparingInt(ContestStandingItem::penalty)
                .thenComparing(item -> item.lastAcAt() == null ? LocalDateTime.MAX : item.lastAcAt()));

        List<ContestStandingItem> ranked = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            ContestStandingItem item = items.get(index);
            ranked.add(new ContestStandingItem(
                    index + 1,
                    item.userId(),
                    item.username(),
                    item.nickname(),
                    item.solved(),
                    item.penalty(),
                    item.lastAcAt(),
                    item.problems()));
        }

        return new ContestStandings(contest.getId(), phase(contest, now), frozen,
                contest.getFreezeAt(), now, ranked);
    }

    private ContestListItem toListItem(Contest contest, Long userId, LocalDateTime now) {
        return new ContestListItem(
                contest.getId(),
                contest.getTitle(),
                contest.getMode(),
                phase(contest, now),
                contest.getStartAt(),
                contest.getEndAt(),
                contest.getFreezeAt(),
                contest.getJoinType(),
                true,
                userId != null && isRegistered(contest.getId(), userId),
                participantCount(contest.getId()),
                problemCount(contest.getId()));
    }

    ContestListItem toAdminListItem(Contest contest, LocalDateTime now) {
        return new ContestListItem(
                contest.getId(),
                contest.getTitle(),
                contest.getMode(),
                phase(contest, now),
                contest.getStartAt(),
                contest.getEndAt(),
                contest.getFreezeAt(),
                contest.getJoinType(),
                contest.getPublished() != null && contest.getPublished() == 1,
                false,
                participantCount(contest.getId()),
                problemCount(contest.getId()));
    }

    public Contest requirePublished(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null || contest.getPublished() == null || contest.getPublished() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "比赛不存在或未发布");
        }
        return contest;
    }

    public boolean isRegistered(Long contestId, Long userId) {
        if (userId == null) {
            return false;
        }
        return participantMapper.selectCount(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId)
                .eq(ContestParticipant::getUserId, userId)) > 0;
    }

    public long participantCount(Long contestId) {
        return participantMapper.selectCount(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId));
    }

    public long problemCount(Long contestId) {
        return contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
    }

    private void requireRegistered(Long contestId, Long userId) {
        if (!isRegistered(contestId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先报名比赛");
        }
    }

    private Map<Long, Problem> problemMap(List<ContestProblem> mappings) {
        List<Long> ids = mappings.stream().map(ContestProblem::getProblemId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));
    }

    private Set<Long> passedProblemIds(Long contestId, Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(Submission::getContestId, contestId)
                .eq(Submission::getStatus, "AC")
                .select(Submission::getProblemId));
        Set<Long> result = new HashSet<>();
        for (Submission submission : submissions) {
            result.add(submission.getProblemId());
        }
        return result;
    }

    private static final class Attempt {
        private LocalDateTime firstAcAt;
        private int wrong;
        private int tries;
    }
}