package com.ssafy.a305.be.domain.notification.dto.response;

import com.ssafy.a305.be.domain.notification.entity.NotificationLog;
import java.time.LocalDateTime;

public record NotificationLogResponseDto(
        Long notificationSeq,
        String eventKey,
        String title,
        String message,
        Boolean isRead,
        LocalDateTime sentDttm,
        Integer contractSeq
) {
    public static NotificationLogResponseDto from(NotificationLog log) {
        return new NotificationLogResponseDto(
                log.getNotificationSeq(),
                log.getEventKey(),
                log.getTitle(),
                log.getMessage(),
                log.getIsRead() != null ? log.getIsRead() : false,
                log.getSentDttm(),
                log.getContract() != null ? log.getContract().getContractSeq() : null
        );
    }
}
