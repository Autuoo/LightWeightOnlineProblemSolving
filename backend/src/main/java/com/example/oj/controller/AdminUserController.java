package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.AdminUserItem;
import com.example.oj.dto.AiConfigView;
import com.example.oj.dto.CreateUserRequest;
import com.example.oj.dto.CreatedUser;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.UserStatusRequest;
import com.example.oj.service.AdminUserService;
import com.example.oj.service.UserAiConfigService;
import jakarta.validation.Valid;
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

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserAiConfigService userAiConfigService;

    public AdminUserController(AdminUserService adminUserService,
                               UserAiConfigService userAiConfigService) {
        this.adminUserService = adminUserService;
        this.userAiConfigService = userAiConfigService;
    }

    @GetMapping
    public Result<PageResponse<AdminUserItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminUserService.list(page, size, keyword));
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
}