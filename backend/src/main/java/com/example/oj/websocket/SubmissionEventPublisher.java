package com.example.oj.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubmissionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public SubmissionEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(Long submissionId, String status, Integer score,
                        Integer execTimeMs, Integer memoryKb, LocalDateTime finishedAt) {
        SubmissionEvent event = new SubmissionEvent(
                "SUBMISSION_STATUS_CHANGED", submissionId, status, score, execTimeMs, memoryKb, finishedAt);
        messagingTemplate.convertAndSend("/topic/submissions/" + submissionId, event);
    }

    public record SubmissionEvent(
            String type,
            Long submissionId,
            String status,
            Integer score,
            Integer execTimeMs,
            Integer memoryKb,
            LocalDateTime finishedAt) {
    }
}