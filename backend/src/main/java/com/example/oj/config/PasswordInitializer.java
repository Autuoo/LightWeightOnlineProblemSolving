package com.example.oj.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.entity.User;
import com.example.oj.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public PasswordInitializer(UserMapper userMapper,
                               PasswordEncoder passwordEncoder,
                               Environment environment) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        String password = environment.getProperty("OJ_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            return;
        }
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        if (admin == null) {
            log.warn("admin user not found, skip password initialization");
            return;
        }
        admin.setPasswordHash(passwordEncoder.encode(password));
        userMapper.updateById(admin);
        log.info("admin password initialized from OJ_ADMIN_PASSWORD");
    }
}