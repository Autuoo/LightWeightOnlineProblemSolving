package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.PageResponse;
import com.example.oj.dto.ProblemDetail;
import com.example.oj.dto.ProblemListItem;
import com.example.oj.service.ProblemService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public Result<PageResponse<ProblemListItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer difficulty,
            Authentication authentication) {
        return Result.ok(problemService.list((Long) authentication.getPrincipal(), page, size, keyword, difficulty));
    }

    @GetMapping("/{id}")
    public Result<ProblemDetail> detail(@PathVariable Long id) {
        return Result.ok(problemService.detail(id));
    }
}