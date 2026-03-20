package com.ssafy.a305.be.domain.notification.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.notification.dto.response.NotificationLogResponseDto;
import com.ssafy.a305.be.domain.notification.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    public ResponseEntity<List<NotificationLogResponseDto>> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(notificationLogService.getNotifications(user.userSeq()));
    }

    @PatchMapping("/{notificationSeq}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long notificationSeq) {
        notificationLogService.markAsRead(notificationSeq, user.userSeq());
        return ResponseEntity.ok().build();
    }
}
