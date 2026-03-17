package com.ssafy.a305.be.domain.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode errorCode = resolveErrorCode(authException);
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .error(errorCode.name())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ErrorCode resolveErrorCode(AuthenticationException authException) {
        if (authException instanceof BadCredentialsException) {
            return ErrorCode.INVALID_ACCESS_TOKEN;
        }
        if (authException instanceof InsufficientAuthenticationException) {
            return ErrorCode.AUTHENTICATION_REQUIRED;
        }
        return ErrorCode.AUTHENTICATION_REQUIRED;
    }
}
