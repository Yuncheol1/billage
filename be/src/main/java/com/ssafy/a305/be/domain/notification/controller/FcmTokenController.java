package com.ssafy.a305.be.domain.notification.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.notification.dto.request.FcmTokenRequestDto;
import com.ssafy.a305.be.domain.notification.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm-tokens")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid FcmTokenRequestDto requestDto) {
        fcmTokenService.registerToken(user.userSeq(), requestDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{tokenSeq}/deactivate")
    public ResponseEntity<Void> deactivateToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tokenSeq) {
        fcmTokenService.deactivateToken(tokenSeq, user.userSeq());
        return ResponseEntity.ok().build();
    }
}
