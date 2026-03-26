package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.contract.dto.request.ChatBotRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ChatBotResponseDto;
import com.ssafy.a305.be.domain.contract.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Tag(name = "ChatBot", description = "AI 챗봇 Q&A API")
public class ChatBotController {

    private final AIService aiService;

    @Operation(summary = "AI 챗봇 질문하기", description = "세법 및 차용증 관련 질문에 대해 AI 답변을 반환합니다.")
    @PostMapping
    public ResponseEntity<ChatBotResponseDto> askChatBot(@Valid @RequestBody ChatBotRequestDto requestDto) {
        log.info("ChatBot interaction request received");
        ChatBotResponseDto response = aiService.getChatResponse(requestDto);
        return ResponseEntity.ok(response);
    }
}
