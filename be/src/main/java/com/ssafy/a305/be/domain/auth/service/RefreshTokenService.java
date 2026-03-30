package com.ssafy.a305.be.domain.auth.service;

import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    public void save(Integer userSeq, String refreshToken, long expiresInSeconds) {
        stringRedisTemplate.opsForValue()
                .set(generateKey(userSeq), refreshToken, Duration.ofSeconds(expiresInSeconds));
    }

    public void validate(Integer userSeq, String refreshToken) {
        String storedToken = stringRedisTemplate.opsForValue().get(generateKey(userSeq));

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomBusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public void rotate(Integer userSeq, String newRefreshToken, long expiresInSeconds) {
        save(userSeq, newRefreshToken, expiresInSeconds);
    }

    public void delete(Integer userSeq) {
        stringRedisTemplate.delete(generateKey(userSeq));
    }

    private String generateKey(Integer userSeq) {
        return KEY_PREFIX + userSeq;
    }
}
