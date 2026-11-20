package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.AdminUserItem;
import com.example.oj.dto.CreateUserRequest;
import com.example.oj.dto.CreatedUser;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.UpdateUserRequest;
import com.example.oj.entity.AiHint;
import com.example.oj.entity.ContestParticipant;
import com.example.oj.entity.Submission;
import com.example.oj.entity.SubmissionCaseResult;
import com.example.oj.entity.User;
import com.example.oj.entity.UserAiConfig;
import com.example.oj.mapper.AiHintMapper;
import com.example.oj.mapper.ContestParticipantMapper;
import com.example.oj.mapper.SubmissionCaseResultMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.UserAiConfigMapper;
import com.example.oj.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminUserService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STUDENT = "STUDENT";

    private final UserMapper userMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionCaseResultMapper submissionCaseResultMapper;
    private final AiHintMapper aiHintMapper;
    private final UserAiConfigMapper userAiConfigMapper;
    private final ContestParticipantMapper contestParticipantMapper;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public AdminUserService(UserMapper userMapper,
                            SubmissionMapper submissionMapper,
                            SubmissionCaseResultMapper submissionCaseResultMapper,
                            AiHintMapper aiHintMapper,
                            UserAiConfigMapper userAiConfigMapper,
                            ContestParticipantMapper contestParticipantMapper,
                            PasswordEncoder passwordEncoder,
                            @Value("${oj.admin.reset-password:12345678}") String defaultPassword) {
        this.userMapper = userMapper;
        this.submissionMapper = submissionMapper;
        this.submissionCaseResultMapper = submissionCaseResultMapper;
        this.aiHintMapper = aiHintMapper;
        this.userAiConfigMapper = userAiConfigMapper;
        this.contestParticipantMapper = contestParticipantMapper;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    public PageResponse<AdminUserItem> list(int pageNo, int size, String keyword) {
        Page<User> page = new Page<>(pageNo, Math.min(size, 100));
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            wrapper.and(item -> item
                    .like(User::getUsername, value)
                    .or()
                    .like(User::getNickname, value));
        }
        wrapper.orderByAsc(User::getId);

        Page<User> result = userMapper.selectPage(page, wrapper);
        List<AdminUserItem> records = result.getRecords().stream()
                .map(user -> new AdminUserItem(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getRole(),
                        user.getStatus(),
                        user.getAvatarUrl(),
                        user.getCreatedAt()))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    public AdminUserItem detail(Long userId) {
        User user = requireUser(userId);
        return new AdminUserItem(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.getAvatarUrl(),
                user.getCreatedAt());
    }

    public void updateStatus(Long operatorId, Long userId, int status) {
        User user = requireUser(userId);
        if (status == 0) {
            if (userId.equals(operatorId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用当前登录账号");
            }
            if (ROLE_ADMIN.equals(user.getRole())) {
                long enabledAdmins = enabledAdminCount();
                if (enabledAdmins <= 1) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "至少保留一个启用状态的管理员");
                }
            }
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, status));
    }

    public void update(Long operatorId, Long userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        String role = request.role() == null || request.role().isBlank()
                ? user.getRole() : request.role().trim().toUpperCase();
        if (!ROLE_ADMIN.equals(role) && !ROLE_STUDENT.equals(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色只能是 ADMIN 或 STUDENT");
        }
        if (userId.equals(operatorId) && !role.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能修改当前登录账号的角色");
        }
        if (ROLE_ADMIN.equals(user.getRole()) && !ROLE_ADMIN.equals(role) && enabledAdminCount() <= 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少保留一个启用状态的管理员");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getRole, role)
                .set(User::getUpdatedAt, LocalDateTime.now());
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            wrapper.set(User::getNickname, nickname.isBlank() ? user.getUsername() : nickname);
        }
        userMapper.update(null, wrapper);
    }

    @Transactional
    public void delete(Long operatorId, Long userId) {
        User user = requireUser(userId);
        if (userId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除当前登录账号");
        }
        if (ROLE_ADMIN.equals(user.getRole())
                && Integer.valueOf(1).equals(user.getStatus())
                && enabledAdminCount() <= 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少保留一个启用状态的管理员");
        }

        List<Long> submissionIds = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .select(Submission::getId)
                        .eq(Submission::getUserId, userId))
                .stream().map(Submission::getId).toList();
        if (!submissionIds.isEmpty()) {
            submissionCaseResultMapper.delete(new LambdaQueryWrapper<SubmissionCaseResult>()
                    .in(SubmissionCaseResult::getSubmissionId, submissionIds));
        }
        submissionMapper.delete(new LambdaQueryWrapper<Submission>().eq(Submission::getUserId, userId));
        aiHintMapper.delete(new LambdaQueryWrapper<AiHint>().eq(AiHint::getUserId, userId));
        userAiConfigMapper.delete(new LambdaQueryWrapper<UserAiConfig>().eq(UserAiConfig::getUserId, userId));
        contestParticipantMapper.delete(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getUserId, userId));
        userMapper.deleteById(userId);
    }

    public String resetPassword(Long userId) {
        requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(defaultPassword)));
        return defaultPassword;
    }

    public CreatedUser create(CreateUserRequest request) {
        String username = request.username().trim();
        long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在：" + username);
        }

        String rawPassword = request.password() == null || request.password().isBlank()
                ? defaultPassword : request.password();
        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? username : request.nickname().trim();

        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole("STUDENT");
        user.setStatus(1);
        userMapper.insert(user);
        return new CreatedUser(user.getId(), username, rawPassword);
    }

    private long enabledAdminCount() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, ROLE_ADMIN)
                .eq(User::getStatus, 1));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}