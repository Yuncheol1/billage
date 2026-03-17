package com.ssafy.a305.be.domain.auth.service;

import com.ssafy.a305.be.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceSessionService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void deactivateFcmToken(Integer userSeq, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        fcmTokenRepository.findByUserUserSeqAndTokenAndIsActiveTrue(userSeq, fcmToken)
                .ifPresent(token -> {
                    token.deactivate();
                    fcmTokenRepository.save(token);
                });
    }
}
