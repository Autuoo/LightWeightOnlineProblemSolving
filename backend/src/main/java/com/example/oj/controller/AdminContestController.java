package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.ContestDetail;
import com.example.oj.dto.ContestListItem;
import com.example.oj.dto.ContestProblemAssignRequest;
import com.example.oj.dto.ContestStandings;
import com.example.oj.dto.ContestUpsertRequest;
import com.example.oj.dto.PageResponse;
import com.example.oj.service.AdminContestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin/contests")
public class AdminContestController {

    private final AdminContestService adminContestService;

    public AdminContestController(AdminContestService adminContestService) {
        this.adminContestService = adminContestService;
    }

    @GetMapping
    public Result<PageResponse<ContestListItem>> list(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String keyword) {
        return Result.ok(adminContestService.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<ContestDetail> detail(@PathVariable Long id) {
        return Result.ok(adminContestService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody ContestUpsertRequest request,
                                            Authentication authentication) {
        Long id = adminContestService.create(request, (Long) authentication.getPrincipal());
        return Result.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContestUpsertRequest request) {
        adminContestService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminContestService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/problems")
    public Result<Void> assignProblems(@PathVariable Long id,
                                       @Valid @RequestBody ContestProblemAssignRequest request) {
        adminContestService.assignProblems(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/participants/{userId}")
    public Result<Void> registerParticipant(@PathVariable Long id, @PathVariable Long userId) {
        adminContestService.registerParticipant(id, userId);
        return Result.ok();
    }

    @GetMapping("/{id}/standings")
    public Result<ContestStandings> standings(@PathVariable Long id) {
        return Result.ok(adminContestService.adminStandings(id));
    }
}