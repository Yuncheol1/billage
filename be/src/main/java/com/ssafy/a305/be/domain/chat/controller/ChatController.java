package com.ssafy.a305.be.domain.chat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto;
import com.ssafy.a305.be.domain.chat.dto.ChatMessageResponseDto;
import com.ssafy.a305.be.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    /**
     * websocket "/pub/chat/message"로 들어오는 메시지를 처리
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageRequestDto message) {
        log.info("Received STOMP message from sender {}: {}", message.senderSeq(), message.content());
        // 1. MongoDB에 메시지 저장
        ChatMessageResponseDto response = chatService.saveMessage(message);
        log.info("Broadcasting message to /sub/chat/room/{}", message.roomId());
        // 2. 해당 방을 구독하고 있는 클라이언트들에게 메시지 브로드캐스팅
        // 목적지: /sub/chat/room/{roomId}
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.roomId(), response);
    }
}
