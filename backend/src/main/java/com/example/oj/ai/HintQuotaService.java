package com.example.oj.ai;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.AiProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class HintQuotaService {

    private final AiProperties properties;
    private final Map<Long, AtomicInteger> userCounts = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastRequestAt = new ConcurrentHashMap<>();
    private final AtomicInteger globalCount = new AtomicInteger();
    private volatile LocalDate currentDay = LocalDate.now();

    public HintQuotaService(AiProperties properties) {
        this.properties = properties;
    }

    public synchronized void checkAndConsume(Long userId) {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            currentDay = today;
            globalCount.set(0);
            userCounts.clear();
            lastRequestAt.clear();
        }

        if (globalCount.get() >= properties.getDailyGlobalQuota()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "今日全局额度已用完");
        }

        long now = System.currentTimeMillis();
        Long last = lastRequestAt.get(userId);
        if (last != null && now - last < properties.getMinIntervalSeconds() * 1000L) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试");
        }

        AtomicInteger count = userCounts.computeIfAbsent(userId, key -> new AtomicInteger());
        if (count.get() >= properties.getDailyUserQuota()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED);
        }

        count.incrementAndGet();
        globalCount.incrementAndGet();
        lastRequestAt.put(userId, now);
    }
}