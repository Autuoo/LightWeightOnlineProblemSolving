package com.example.oj.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.PasswordChangeRequest;
import com.example.oj.dto.ProfileUpdateRequest;
import com.example.oj.dto.ProfileView;
import com.example.oj.entity.User;
import com.example.oj.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Service
public class ProfileService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final String AVATAR_URL_PREFIX = "/uploads/";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Path uploadRoot;

    public ProfileService(UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          @Value("${oj.upload.dir:./uploads}") String uploadDir) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public ProfileView view(Long userId) {
        return toView(requireUser(userId));
    }

    public ProfileView updateNickname(Long userId, ProfileUpdateRequest request) {
        User user = requireUser(userId);
        String nickname = request.nickname().trim();
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, nickname));
        user.setNickname(nickname);
        return toView(user);
    }

    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前密码不正确");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(request.newPassword())));
    }

    public String updateAvatar(Long userId, MultipartFile file) {
        User user = requireUser(userId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片文件");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像不能超过 2MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 png / jpg / jpeg / webp");
        }

        Path avatarDir = uploadRoot.resolve("avatars");
        try {
            Files.createDirectories(avatarDir);
            String filename = "u" + userId + "_" + System.currentTimeMillis() + "." + extension;
            file.transferTo(avatarDir.resolve(filename));
            removeOldAvatar(user.getAvatarUrl());
            String url = AVATAR_URL_PREFIX + "avatars/" + filename;
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getAvatarUrl, url));
            return url;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像保存失败");
        }
    }

    public void removeAvatar(Long userId) {
        User user = requireUser(userId);
        removeOldAvatar(user.getAvatarUrl());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getAvatarUrl, null));
    }

    private void removeOldAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }
        String relative = avatarUrl.substring(AVATAR_URL_PREFIX.length());
        Path target = uploadRoot.resolve(relative).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    private ProfileView toView(User user) {
        return new ProfileView(user.getId(), user.getUsername(), user.getNickname(),
                user.getRole(), user.getAvatarUrl(), user.getCreatedAt());
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}