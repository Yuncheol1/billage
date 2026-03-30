package com.ssafy.a305.be.domain.notification.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.notification.dto.response.NotificationLogResponseDto;
import com.ssafy.a305.be.domain.notification.dto.response.UnreadCountResponseDto;
import com.ssafy.a305.be.domain.notification.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<NotificationLogResponseDto> notifications = notificationLogService.getNotifications(user.userSeq());
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "알림 목록 조회 성공",
                "data", notifications
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser user) {
        UnreadCountResponseDto unreadCount = notificationLogService.getUnreadCount(user.userSeq());
        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "안 읽은 알림 수 조회 성공",
                "data", unreadCount
        ));
    }

    @PatchMapping("/{notificationSeq}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long notificationSeq) {
        notificationLogService.markAsRead(notificationSeq, user.userSeq());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("message", "알림 읽음 처리 성공");
        response.put("data", null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser user) {
        notificationLogService.markAllAsRead(user.userSeq());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("message", "전체 알림 읽음 처리 성공");
        response.put("data", null);
        return ResponseEntity.ok(response);
    }
}
