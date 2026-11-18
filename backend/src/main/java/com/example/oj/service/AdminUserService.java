package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.AdminUserItem;
import com.example.oj.dto.PageResponse;
import com.example.oj.entity.User;
import com.example.oj.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public AdminUserService(UserMapper userMapper,
                            PasswordEncoder passwordEncoder,
                            @Value("${oj.admin.reset-password:12345678}") String defaultPassword) {
        this.userMapper = userMapper;
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
                        user.getCreatedAt()))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    public void updateStatus(Long operatorId, Long userId, int status) {
        User user = requireUser(userId);
        if (status == 0) {
            if (userId.equals(operatorId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用当前登录账号");
            }
            if (ROLE_ADMIN.equals(user.getRole())) {
                long enabledAdmins = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, ROLE_ADMIN)
                        .eq(User::getStatus, 1));
                if (enabledAdmins <= 1) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "至少保留一个启用状态的管理员");
                }
            }
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, status));
    }

    public String resetPassword(Long userId) {
        requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(defaultPassword)));
        return defaultPassword;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}