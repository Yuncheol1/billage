package com.ssafy.a305.be.domain.chat.dto;

import java.time.LocalDateTime;
import com.ssafy.a305.be.domain.chat.entity.ChatMessage;

public record ChatMessageResponseDto(
    String id,
    String roomId,
    Integer senderSeq,
    String senderName,
    String content,
    Boolean isContractMsg,
    String msgType,
    LocalDateTime createdAt
) {
    public static ChatMessageResponseDto of(ChatMessage message, String senderName) {
        return new ChatMessageResponseDto(
                message.getId(),
                message.getRoomId(),
                message.getSenderSeq(),
                senderName,
                message.getContent(),
                message.getIsContractMsg(),
                message.getMsgType(),
                message.getCreatedAt()
        );
    }
}
