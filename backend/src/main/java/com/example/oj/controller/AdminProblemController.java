package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.AdminProblemDetail;
import com.example.oj.dto.AdminProblemItem;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.ProblemUpsertRequest;
import com.example.oj.dto.TestCaseBatchRequest;
import com.example.oj.service.AdminProblemService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/problems")
public class AdminProblemController {

    private final AdminProblemService adminProblemService;

    public AdminProblemController(AdminProblemService adminProblemService) {
        this.adminProblemService = adminProblemService;
    }

    @GetMapping
    public Result<PageResponse<AdminProblemItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer difficulty) {
        return Result.ok(adminProblemService.list(page, size, keyword, difficulty));
    }

    @GetMapping("/{id}")
    public Result<AdminProblemDetail> detail(@PathVariable Long id) {
        return Result.ok(adminProblemService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody ProblemUpsertRequest request,
                                            Authentication authentication) {
        Long id = adminProblemService.create(request, (Long) authentication.getPrincipal());
        return Result.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProblemUpsertRequest request) {
        adminProblemService.update(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/test-cases")
    public Result<Void> replaceTestCases(@PathVariable Long id,
                                         @Valid @RequestBody TestCaseBatchRequest request) {
        adminProblemService.replaceTestCases(id, request);
        return Result.ok();
    }
}