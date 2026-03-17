package com.ssafy.a305.be.domain.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.ACCESS_DENIED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), ErrorResponse.builder()
                .status(ErrorCode.ACCESS_DENIED.getStatus().value())
                .error(ErrorCode.ACCESS_DENIED.name())
                .message(ErrorCode.ACCESS_DENIED.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
