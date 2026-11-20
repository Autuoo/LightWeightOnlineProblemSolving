package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.AdminUserItem;
import com.example.oj.dto.AiConfigView;
import com.example.oj.dto.CreateUserRequest;
import com.example.oj.dto.CreatedUser;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.UpdateUserRequest;
import com.example.oj.dto.UserStatusRequest;
import com.example.oj.service.AdminUserService;
import com.example.oj.service.ProfileService;
import com.example.oj.service.UserAiConfigService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserAiConfigService userAiConfigService;
    private final ProfileService profileService;

    public AdminUserController(AdminUserService adminUserService,
                               UserAiConfigService userAiConfigService,
                               ProfileService profileService) {
        this.adminUserService = adminUserService;
        this.userAiConfigService = userAiConfigService;
        this.profileService = profileService;
    }

    @GetMapping
    public Result<PageResponse<AdminUserItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminUserService.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<AdminUserItem> detail(@PathVariable Long id) {
        return Result.ok(adminUserService.detail(id));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UserStatusRequest request,
                                     Authentication authentication) {
        adminUserService.updateStatus((Long) authentication.getPrincipal(), id, request.status());
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        return Result.ok(Map.of("password", adminUserService.resetPassword(id)));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request,
                               Authentication authentication) {
        adminUserService.update((Long) authentication.getPrincipal(), id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        adminUserService.delete((Long) authentication.getPrincipal(), id);
        return Result.ok();
    }

    @PostMapping
    public Result<CreatedUser> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.ok(adminUserService.create(request));
    }

    @GetMapping("/{id}/ai-config")
    public Result<AiConfigView> aiConfig(@PathVariable Long id) {
        return Result.ok(userAiConfigService.view(id));
    }

    @DeleteMapping("/{id}/ai-config")
    public Result<Void> clearAiConfig(@PathVariable Long id) {
        userAiConfigService.clear(id);
        return Result.ok();
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadAvatar(@PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file) {
        return Result.ok(Map.of("avatarUrl", profileService.updateAvatar(id, file)));
    }

    @DeleteMapping("/{id}/avatar")
    public Result<Void> removeAvatar(@PathVariable Long id) {
        profileService.removeAvatar(id);
        return Result.ok();
    }
}