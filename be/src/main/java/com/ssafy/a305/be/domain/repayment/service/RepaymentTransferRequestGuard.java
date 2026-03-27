package com.ssafy.a305.be.domain.repayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RepaymentTransferRequestGuard {
    private static final String KEY_PREFIX = "repay:transfer:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public String tryAcquire(Integer userSeq, Integer contractSeq, Integer scheduleSeq) {
        String key = generateKey(userSeq, contractSeq, scheduleSeq);
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void release(Integer userSeq, Integer contractSeq, Integer scheduleSeq, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String key = generateKey(userSeq, contractSeq, scheduleSeq);
        stringRedisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), token);
    }

    private String generateKey(Integer userSeq, Integer contractSeq, Integer scheduleSeq) {
        return KEY_PREFIX + userSeq + ":" + contractSeq + ":" + scheduleSeq;
    }
}
