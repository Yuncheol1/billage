package com.ssafy.a305.be.domain.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.a305.be.domain.chat.dto.ChatMessageResponseDto;
import com.ssafy.a305.be.domain.chat.dto.ChatRoomCreateRequestDto;
import com.ssafy.a305.be.domain.chat.dto.ChatRoomResponseDto;
import com.ssafy.a305.be.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    // 내 채팅방 목록 조회 (임시로 헤더나 파라미터로 userSeq를 받는다고 가정)
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyChatRooms(
            @RequestHeader(value = "userSeq", defaultValue = "1") Integer userSeq) {
        // 실제로는 SecurityContextHolder 등에서 추출해야 함
        return ResponseEntity.ok(chatService.getRoomsByUserSeq(userSeq));
    }

    // 특정 채팅방의 과거 메시지 조회
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getChatMessages(@PathVariable("roomId") String roomId) {
        log.info("채팅 메시지 내역 조회 요청: roomId={}", roomId);
        List<ChatMessageResponseDto> messages = chatService.getMessagesByRoomId(roomId);
        return ResponseEntity.ok(messages);
    }

    // 이메일로 새로운 채팅방 생성 (혹은 기존 방 오픈)
    @org.springframework.web.bind.annotation.PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponseDto> createRoomByEmail(
            @RequestHeader(value = "userSeq", defaultValue = "1") Integer userSeq,
            @org.springframework.web.bind.annotation.RequestBody ChatRoomCreateRequestDto request) {
        // 실제로는 인증 정보에서 userSeq를 추출
        ChatRoomResponseDto room = chatService.createRoomByEmail(userSeq, request.targetEmail());
        return ResponseEntity.ok(room);
    }

    // 채팅방 나가기
    @org.springframework.web.bind.annotation.PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @RequestHeader(value = "userSeq", defaultValue = "1") Integer userSeq,
            @PathVariable("roomId") String roomId) {
        chatService.leaveRoom(userSeq, roomId);
        return ResponseEntity.ok().build();
    }
}
