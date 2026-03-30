package com.ssafy.a305.be.domain.notification.service;

import com.ssafy.a305.be.domain.notification.dto.request.FcmTokenRequestDto;
import com.ssafy.a305.be.domain.notification.entity.FcmToken;
import com.ssafy.a305.be.domain.notification.repository.FcmTokenRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Integer userSeq, FcmTokenRequestDto requestDto) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String tokenString = requestDto.getToken();

        fcmTokenRepository.findByUserUserSeqAndToken(userSeq, tokenString)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(tokenString),
                        () -> {
                            FcmToken newToken = FcmToken.builder()
                                    .user(user)
                                    .token(tokenString)
                                    .build();
                            fcmTokenRepository.save(newToken);
                        }
                );
        log.info("FCM Token registered for user: {}", userSeq);
    }

    @Transactional
    public void deactivateToken(Long tokenSeq, Integer userSeq) {
        FcmToken token = fcmTokenRepository.findById(tokenSeq)
                .orElseThrow(() -> new IllegalArgumentException("토큰을 찾을 수 없습니다."));

        if (!token.getUser().getUserSeq().equals(userSeq)) {
            throw new IllegalArgumentException("본인의 토큰만 비활성화할 수 있습니다.");
        }

        token.deactivate();
        log.info("FCM Token deactivated for user: {}", userSeq);
    }
}
