package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.PasswordChangeRequest;
import com.example.oj.dto.ProfileUpdateRequest;
import com.example.oj.dto.ProfileView;
import com.example.oj.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public Result<ProfileView> profile(Authentication authentication) {
        return Result.ok(profileService.view((Long) authentication.getPrincipal()));
    }

    @PutMapping("/profile")
    public Result<ProfileView> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                             Authentication authentication) {
        return Result.ok(profileService.updateNickname((Long) authentication.getPrincipal(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                       Authentication authentication) {
        profileService.changePassword((Long) authentication.getPrincipal(), request);
        return Result.ok();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                    Authentication authentication) {
        String url = profileService.updateAvatar((Long) authentication.getPrincipal(), file);
        return Result.ok(Map.of("avatarUrl", url));
    }

    @DeleteMapping("/avatar")
    public Result<Void> removeAvatar(Authentication authentication) {
        profileService.removeAvatar((Long) authentication.getPrincipal());
        return Result.ok();
    }
}