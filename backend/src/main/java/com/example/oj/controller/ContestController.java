package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.ContestDetail;
import com.example.oj.dto.ContestListItem;
import com.example.oj.dto.ContestProblemItem;
import com.example.oj.dto.ContestRegisterRequest;
import com.example.oj.dto.ContestStandings;
import com.example.oj.dto.ProblemDetail;
import com.example.oj.service.ContestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    public Result<List<ContestListItem>> list(@RequestParam(required = false) String keyword,
                                              Authentication authentication) {
        return Result.ok(contestService.list(userId(authentication), keyword));
    }

    @GetMapping("/{id}")
    public Result<ContestDetail> detail(@PathVariable Long id, Authentication authentication) {
        return Result.ok(contestService.detail(id, userId(authentication)));
    }

    @PostMapping("/{id}/register")
    public Result<Void> register(@PathVariable Long id,
                                 @Valid @RequestBody(required = false) ContestRegisterRequest request,
                                 Authentication authentication) {
        contestService.register(id, userId(authentication), request);
        return Result.ok();
    }

    @GetMapping("/{id}/problems")
    public Result<List<ContestProblemItem>> problems(@PathVariable Long id, Authentication authentication) {
        return Result.ok(contestService.detail(id, userId(authentication)).problems());
    }

    @GetMapping("/{id}/problems/{problemId}")
    public Result<ProblemDetail> problem(@PathVariable Long id, @PathVariable Long problemId,
                                         Authentication authentication) {
        return Result.ok(contestService.problemDetail(id, problemId, userId(authentication)));
    }

    @GetMapping("/{id}/standings")
    public Result<ContestStandings> standings(@PathVariable Long id, Authentication authentication) {
        return Result.ok(contestService.standings(id, userId(authentication)));
    }

    private Long userId(Authentication authentication) {
        return authentication == null ? null : (Long) authentication.getPrincipal();
    }
}