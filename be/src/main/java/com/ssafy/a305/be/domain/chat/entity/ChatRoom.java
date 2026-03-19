package com.ssafy.a305.be.domain.chat.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "chat_rooms")
public class ChatRoom {

    @Id
    private String id;
    
    // 참여자 userSeq 목록 (1:1 이므로 보통 2개)
    private List<Integer> participants;
    
    // 마지막 메시지 요약 (채팅방 목록 표시용)
    private String lastMessage;
    
    @Builder.Default
    private LocalDateTime lastModifiedAt = LocalDateTime.now();
    
    // 유저별 마지막 읽은 시각 저장 (MongoDB Map 키는 String이어야 함)
    @Builder.Default
    private java.util.Map<String, LocalDateTime> lastReadAt = new java.util.HashMap<>();
    
    public void updateLastMessage(String message) {
        this.lastMessage = message;
        this.lastModifiedAt = LocalDateTime.now();
    }
    
    public void updateLastMessage(String message, LocalDateTime time) {
        this.lastMessage = message;
        this.lastModifiedAt = time;
    }

    public void updateParticipants(List<Integer> participants) {
        this.participants = participants;
    }
}
