package com.ssafy.a305.be.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ssafy.a305.be.domain.notification.dto.ChatNotificationEvent;
import com.ssafy.a305.be.domain.notification.entity.FcmToken;
import com.ssafy.a305.be.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmMessageService {

    private final FcmTokenRepository fcmTokenRepository;

    @EventListener
    public void handleChatNotificationEvent(ChatNotificationEvent event) {
        log.info("Handling chat notification event for recipientSeq: {}", event.recipientSeq());
        
        List<FcmToken> tokens = fcmTokenRepository.findAllByUserUserSeqAndIsActiveTrue(event.recipientSeq());
        
        if (tokens.isEmpty()) {
            log.debug("No active FCM tokens found for userSeq: {}", event.recipientSeq());
            return;
        }

        // 알림 내용을 100자 이내로 자르기 (푸시 알림용)
        String body = event.content();
        if (body != null && body.length() > 100) {
            body = body.substring(0, 97) + "...";
        }

        for (FcmToken fcmToken : tokens) {
            try {
                Message message = Message.builder()
                        .setNotification(Notification.builder()
                                .setTitle(event.senderName())
                                .setBody(body)
                                .build())
                        .putData("type", "CHAT")
                        .putData("roomId", event.roomId())
                        .setToken(fcmToken.getToken())
                        .build();

                // 비동기로 FCM 푸시 발송
                FirebaseMessaging.getInstance().sendAsync(message);
                log.info("FCM chat push enqueued to token: {}", fcmToken.getToken());
            } catch (Exception e) {
                log.error("Failed to enqueue FCM message to token: {}", fcmToken.getToken(), e);
            }
        }
    }
}
