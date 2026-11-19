package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.LoginRequest;
import com.example.oj.dto.LoginResponse;
import com.example.oj.dto.RegisterRequest;
import com.example.oj.entity.User;
import com.example.oj.mapper.UserMapper;
import com.example.oj.security.JwtTokenProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已禁用");
        }
        return issue(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = request.username().trim();
        long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已被注册");
        }

        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? username : request.nickname().trim();
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("STUDENT");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已被注册");
        }
        return issue(user);
    }

    private LoginResponse issue(User user) {
        String token = tokenProvider.createToken(user.getId(), user.getRole());
        return new LoginResponse(token, tokenProvider.getExpireSeconds(), toView(user));
    }

    public LoginResponse.UserView me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toView(user);
    }

    private LoginResponse.UserView toView(User user) {
        return new LoginResponse.UserView(user.getId(), user.getUsername(), user.getNickname(),
                user.getRole(), user.getAvatarUrl());
    }
}