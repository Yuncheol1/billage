package com.ssafy.a305.be.domain.notification.service;

import com.ssafy.a305.be.domain.notification.dto.response.NotificationLogResponseDto;
import com.ssafy.a305.be.domain.notification.entity.NotificationLog;
import com.ssafy.a305.be.domain.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    public List<NotificationLogResponseDto> getNotifications(Integer userSeq) {
        return notificationLogRepository.findByUserUserSeqOrderBySentDttmDesc(userSeq)
                .stream()
                .map(NotificationLogResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationSeq, Integer userSeq) {
        NotificationLog log = notificationLogRepository.findById(notificationSeq)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!log.getUser().getUserSeq().equals(userSeq)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        log.markAsRead();
    }
}
