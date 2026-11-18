package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.oj.dto.LeaderboardItem;
import com.example.oj.entity.Submission;
import com.example.oj.entity.User;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;

    public LeaderboardService(SubmissionMapper submissionMapper, UserMapper userMapper) {
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
    }

    public List<LeaderboardItem> list(int limit) {
        int size = Math.min(Math.max(limit, 1), 100);

        QueryWrapper<Submission> query = new QueryWrapper<>();
        query.select(
                "user_id AS userId",
                "COUNT(DISTINCT CASE WHEN status = 'AC' THEN problem_id END) AS solvedCount",
                "COUNT(*) AS submissionCount",
                "SUM(CASE WHEN status = 'AC' THEN 1 ELSE 0 END) AS acceptedCount");
        query.groupBy("user_id");
        query.last("ORDER BY solvedCount DESC, submissionCount ASC LIMIT " + size);

        List<Map<String, Object>> rows = submissionMapper.selectMaps(query);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = rows.stream()
                .map(row -> toLong(row.get("userId")))
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<LeaderboardItem> items = new ArrayList<>(rows.size());
        int rank = 1;
        for (Map<String, Object> row : rows) {
            Long userId = toLong(row.get("userId"));
            if (userId == null) {
                continue;
            }
            User user = userMap.get(userId);
            int solved = toInt(row.get("solvedCount"));
            int submissions = toInt(row.get("submissionCount"));
            int accepted = toInt(row.get("acceptedCount"));
            double rate = submissions == 0 ? 0.0 : (double) accepted / submissions;
            items.add(new LeaderboardItem(
                    rank++,
                    userId,
                    user == null ? "-" : user.getUsername(),
                    user == null ? "-" : user.getNickname(),
                    solved,
                    submissions,
                    accepted,
                    Math.round(rate * 10000) / 10000.0));
        }
        return items;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}