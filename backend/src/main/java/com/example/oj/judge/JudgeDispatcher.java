package com.example.oj.judge;

import com.example.oj.entity.Submission;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.websocket.SubmissionEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class JudgeDispatcher {

    private final ThreadPoolExecutor judgeExecutor;
    private final JudgeEngine judgeEngine;
    private final SubmissionMapper submissionMapper;
    private final SubmissionEventPublisher eventPublisher;

    public JudgeDispatcher(@Qualifier("judgeExecutor") ThreadPoolExecutor judgeExecutor,
                           JudgeEngine judgeEngine,
                           SubmissionMapper submissionMapper,
                           SubmissionEventPublisher eventPublisher) {
        this.judgeExecutor = judgeExecutor;
        this.judgeEngine = judgeEngine;
        this.submissionMapper = submissionMapper;
        this.eventPublisher = eventPublisher;
    }

    public boolean dispatch(JudgeTask task) {
        if (judgeExecutor.getQueue().remainingCapacity() == 0) {
            return false;
        }
        try {
            judgeExecutor.execute(() -> {
                judgeEngine.judge(task);
                publishFinal(task.submissionId());
            });
            return true;
        } catch (RejectedExecutionException ex) {
            return false;
        }
    }

    private void publishFinal(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getFinishedAt() == null) {
            return;
        }
        eventPublisher.publish(submissionId, submission.getStatus(), submission.getScore(),
                submission.getExecTimeMs(), submission.getMemoryKb(), submission.getFinishedAt());
    }

    public int queueSize() {
        return judgeExecutor.getQueue().size();
    }

    public int activeCount() {
        return judgeExecutor.getActiveCount();
    }
}