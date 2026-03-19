package com.ssafy.a305.be.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmTokenRequestDto {
    
    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String token;
}
