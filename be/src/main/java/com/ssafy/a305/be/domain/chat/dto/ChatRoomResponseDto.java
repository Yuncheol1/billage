package com.ssafy.a305.be.domain.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.ssafy.a305.be.domain.chat.entity.ChatRoom;

public record ChatRoomResponseDto(
    String id,
    List<ParticipantDto> participants,
    String lastMessage,
    LocalDateTime lastModifiedAt,
    Long unreadCount
) {
    public static ChatRoomResponseDto of(ChatRoom room, List<ParticipantDto> participants, Long unreadCount) {
        return new ChatRoomResponseDto(
                room.getId(),
                participants,
                room.getLastMessage(),
                room.getLastModifiedAt(),
                unreadCount
        );
    }
}
