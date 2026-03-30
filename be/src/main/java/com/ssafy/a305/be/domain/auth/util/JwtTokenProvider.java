package com.ssafy.a305.be.domain.auth.util;

import com.ssafy.a305.be.domain.auth.config.JwtProperties;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    @PostConstruct
    void initialize() {
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().length() < 32) {
            throw new CustomBusinessException(ErrorCode.AUTH_CONFIG_INVALID, "JWT_SECRET은 32자 이상이어야 합니다.");
        }

        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Integer userSeq) {
        return createToken(userSeq, "ACCESS", jwtProperties.getAccessTokenExpirationSeconds());
    }

    public String createRefreshToken(Integer userSeq) {
        return createToken(userSeq, "REFRESH", jwtProperties.getRefreshTokenExpirationSeconds());
    }

    public void validateAccessToken(String token) {
        validateTokenType(token, "ACCESS", ErrorCode.INVALID_ACCESS_TOKEN);
    }

    public void validateRefreshToken(String token) {
        validateTokenType(token, "REFRESH", ErrorCode.INVALID_REFRESH_TOKEN);
    }

    public Integer extractUserSeq(String token) {
        Object userSeq = parseClaims(token, ErrorCode.INVALID_ACCESS_TOKEN).get("userSeq");

        if (userSeq instanceof Integer integerUserSeq) {
            return integerUserSeq;
        }
        if (userSeq instanceof Number numberUserSeq) {
            return numberUserSeq.intValue();
        }
        if (userSeq instanceof String stringUserSeq) {
            return Integer.valueOf(stringUserSeq);
        }

        throw new BadCredentialsException(ErrorCode.INVALID_ACCESS_TOKEN.getMessage());
    }

    public Integer extractUserSeqFromRefreshToken(String token) {
        Object userSeq = parseClaims(token, ErrorCode.INVALID_REFRESH_TOKEN).get("userSeq");

        if (userSeq instanceof Integer integerUserSeq) {
            return integerUserSeq;
        }
        if (userSeq instanceof Number numberUserSeq) {
            return numberUserSeq.intValue();
        }
        if (userSeq instanceof String stringUserSeq) {
            return Integer.valueOf(stringUserSeq);
        }

        throw new BadCredentialsException(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    private void validateTokenType(String token, String expectedType, ErrorCode errorCode) {
        Claims claims = parseClaims(token, errorCode);
        Object tokenType = claims.get("type");

        if (!expectedType.equals(tokenType)) {
            throw new BadCredentialsException(errorCode.getMessage());
        }
    }

    private String createToken(Integer userSeq, String tokenType, long expirationSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(String.valueOf(userSeq))
                .issuer(jwtProperties.getIssuer())
                .claim("userSeq", userSeq)
                .claim("type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token, ErrorCode errorCode) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException(errorCode.getMessage(), e);
        }
    }
}
