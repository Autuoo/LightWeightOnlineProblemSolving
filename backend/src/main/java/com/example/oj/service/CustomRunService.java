package com.example.oj.service;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.CustomRunRequest;
import com.example.oj.dto.CustomRunResponse;
import com.example.oj.entity.Problem;
import com.example.oj.judge.sandbox.SandboxResult;
import com.example.oj.judge.sandbox.SandboxRunner;
import com.example.oj.judge.sandbox.SandboxSpec;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomRunService {

    private static final List<String> LANGUAGES = List.of("PYTHON3", "CPP17", "JAVA17");
    private static final int MAX_OUTPUT_CHARS = 65536;

    private final ProblemService problemService;
    private final SandboxRunner sandboxRunner;

    public CustomRunService(ProblemService problemService, SandboxRunner sandboxRunner) {
        this.problemService = problemService;
        this.sandboxRunner = sandboxRunner;
    }

    public CustomRunResponse run(CustomRunRequest request) {
        if (!LANGUAGES.contains(request.language())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_LANGUAGE);
        }
        Problem problem = problemService.requireVisible(request.problemId());

        int timeLimitMs = problem.getTimeLimitMs() == null ? 1000 : problem.getTimeLimitMs();
        int memoryLimitMb = problem.getMemoryLimitMb() == null ? 256 : problem.getMemoryLimitMb();
        SandboxResult result = sandboxRunner.run(new SandboxSpec(
                request.language(),
                request.code(),
                request.input() == null ? "" : request.input(),
                timeLimitMs,
                memoryLimitMb));

        String compileOutput = truncate(result.compileOutput());
        String stdout = truncate(result.stdout());
        String stderr = truncate(result.stderr());

        String status;
        if (!compileOutput.isBlank()) {
            status = "CE";
        } else if (result.timedOut()) {
            status = "TLE";
        } else if (result.exitCode() == 0) {
            status = "OK";
        } else {
            status = "RE";
        }

        return new CustomRunResponse(
                status,
                result.exitCode(),
                result.elapsedMs(),
                result.peakMemoryKb(),
                stdout,
                stderr,
                compileOutput);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= MAX_OUTPUT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_OUTPUT_CHARS) + "\n...[输出过长已截断]";
    }
}