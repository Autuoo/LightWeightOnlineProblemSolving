package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.dto.AdminSubmissionItem;
import com.example.oj.dto.PageResponse;
import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.entity.User;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminSubmissionService {

    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;

    public AdminSubmissionService(SubmissionMapper submissionMapper,
                                  UserMapper userMapper,
                                  ProblemMapper problemMapper) {
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
    }

    public PageResponse<AdminSubmissionItem> list(int pageNo, int size, Long userId, String status,
                                                  String keyword, Boolean suspicious,
                                                  LocalDateTime from, LocalDateTime to) {
        Page<Submission> page = new Page<>(pageNo, Math.min(size, 100));
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(userId != null, Submission::getUserId, userId)
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
        List<Submission> records = result.getRecords();
        Map<Long, String> usernames = usernames(records);
        Map<Long, String> titles = titles(records);

        List<AdminSubmissionItem> items = records.stream()
                .map(item -> new AdminSubmissionItem(
                        item.getId(),
                        item.getUserId(),
                        usernames.getOrDefault(item.getUserId(), ""),
                        titles.getOrDefault(item.getProblemId(), ""),
                        item.getLanguage(),
                        item.getStatus(),
                        item.getScore(),
                        item.getCreatedAt(),
                        item.getSuspicious() != null && item.getSuspicious() == 1))
                .toList();
        return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    private Map<Long, String> usernames(List<Submission> records) {
        List<Long> ids = records.stream().map(Submission::getUserId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private Map<Long, String> titles(List<Submission> records) {
        List<Long> ids = records.stream().map(Submission::getProblemId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle, (a, b) -> a));
    }
}