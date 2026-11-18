package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.SubmissionDetail;
import com.example.oj.dto.SubmissionListItem;
import com.example.oj.dto.SubmitRequest;
import com.example.oj.dto.SubmitResponse;
import com.example.oj.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public Result<SubmitResponse> submit(@Valid @RequestBody SubmitRequest request, Authentication authentication) {
        return Result.ok(submissionService.submit((Long) authentication.getPrincipal(), request));
    }

    @GetMapping("/{id}")
    public Result<SubmissionDetail> detail(@PathVariable Long id, Authentication authentication) {
        return Result.ok(submissionService.detail(
                (Long) authentication.getPrincipal(), isAdmin(authentication), id));
    }

    @GetMapping
    public Result<PageResponse<SubmissionListItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        return Result.ok(submissionService.list(
                (Long) authentication.getPrincipal(), page, size, problemId, status, keyword,
                fromTime, toTime));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}