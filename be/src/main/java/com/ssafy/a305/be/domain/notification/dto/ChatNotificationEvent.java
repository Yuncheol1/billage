package com.ssafy.a305.be.domain.notification.dto;

public record ChatNotificationEvent(
    String roomId,
    Integer senderSeq,
    Integer recipientSeq,
    String senderName,
    String content
) {
}
