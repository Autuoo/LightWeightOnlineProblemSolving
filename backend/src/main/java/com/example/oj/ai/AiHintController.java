package com.example.oj.ai;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.AiProperties;
import com.example.oj.dto.HintRequest;
import com.example.oj.entity.AiHint;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.AiHintMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/submissions")
public class AiHintController {

    private static final long SSE_TIMEOUT_MS = 35_000L;
    private static final int CHUNK_SIZE = 12;
    private static final Set<String> TERMINAL_STATUSES =
            Set.of("AC", "WA", "TLE", "MLE", "RE", "CE", "REJECTED");

    private final AiProperties aiProperties;
    private final SubmissionMapper submissionMapper;
    private final AiHintMapper aiHintMapper;
    private final HintQuotaService quotaService;
    private final AgentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final ExecutorService aiExecutor;

    public AiHintController(AiProperties aiProperties,
                            SubmissionMapper submissionMapper,
                            AiHintMapper aiHintMapper,
                            HintQuotaService quotaService,
                            AgentOrchestrator orchestrator,
                            ObjectMapper objectMapper,
                            @Qualifier("aiExecutor") ExecutorService aiExecutor) {
        this.aiProperties = aiProperties;
        this.submissionMapper = submissionMapper;
        this.aiHintMapper = aiHintMapper;
        this.quotaService = quotaService;
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
        this.aiExecutor = aiExecutor;
    }

    @PostMapping(value = "/{id}/hint", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter hint(@PathVariable Long id,
                           @RequestBody(required = false) HintRequest request,
                           Authentication authentication) {
        if (!aiProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 助教未启用");
        }

        Long userId = (Long) authentication.getPrincipal();
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!submission.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!TERMINAL_STATUSES.contains(submission.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "提交尚未评测完成");
        }

        String question = resolveQuestion(request);
        quotaService.checkAndConsume(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        HintContext context = new HintContext(userId, submission.getProblemId(), submission.getId());

        aiExecutor.execute(() -> runHint(emitter, context, submission, question));
        return emitter;
    }

    private void runHint(SseEmitter emitter, HintContext context, Submission submission, String question) {
        try {
            HintResult result = orchestrator.orchestrate(context, question, trace -> {
                try {
                    emitter.send(SseEmitter.event().name("tool_call").data(Map.of(
                            "tool", trace.name(),
                            "ok", trace.ok(),
                            "elapsedMs", trace.elapsedMs())));
                } catch (IOException ignored) {
                }
            });

            Long hintId = saveHint(submission, question, result);
            for (String chunk : chunks(result.content())) {
                emitter.send(SseEmitter.event().name("token").data(Map.of("text", chunk)));
            }
            emitter.send(SseEmitter.event().name("done").data(Map.of(
                    "hintId", hintId,
                    "status", result.status(),
                    "tokens", result.promptTokens() + result.completionTokens())));
            emitter.complete();
        } catch (BusinessException ex) {
            sendError(emitter, ex);
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private Long saveHint(Submission submission, String question, HintResult result) throws IOException {
        AiHint hint = new AiHint();
        hint.setSubmissionId(submission.getId());
        hint.setUserId(submission.getUserId());
        hint.setQuestion(question.length() > 500 ? question.substring(0, 500) : question);
        hint.setAnswer(result.content());
        hint.setToolCalls(objectMapper.writeValueAsString(result.toolTraces()));
        hint.setModel(aiProperties.getModel());
        hint.setPromptTokens(result.promptTokens());
        hint.setCompletionTokens(result.completionTokens());
        hint.setStepCount(result.steps());
        hint.setStatus(result.status());
        aiHintMapper.insert(hint);
        return hint.getId();
    }

    private void sendError(SseEmitter emitter, BusinessException ex) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of(
                    "code", ex.getErrorCode().getCode(),
                    "message", ex.getMessage())));
            emitter.complete();
        } catch (IOException ignored) {
            emitter.completeWithError(ex);
        }
    }

    private String resolveQuestion(HintRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return "请分析这次提交为什么没有通过";
        }
        return request.question().trim();
    }

    private List<String> chunks(String content) {
        if (content == null || content.isEmpty()) {
            return List.of("");
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
            chunks.add(content.substring(i, Math.min(content.length(), i + CHUNK_SIZE)));
        }
        return chunks;
    }
}