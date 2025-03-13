package com.patrol.api.notification.controller;

import com.patrol.api.notification.dto.NotificationResponse;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.service.MemberService;
import com.patrol.domain.notification.service.NotificationService;
import com.patrol.global.webMvc.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 API", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 조회합니다.")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @LoginUser Member loginUser,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponse> notifications = notificationService.getNotifications(loginUser, pageable);

        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ResponseEntity<Void> markAsRead(
            @LoginUser Member loginUser,
            @PathVariable Long notificationId) {

        notificationService.markAsRead(notificationId, loginUser);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    public ResponseEntity<Void> deleteNotification(
            @LoginUser Member loginUser,
            @PathVariable Long notificationId) {

        notificationService.deleteNotification(notificationId);

        return ResponseEntity.ok().build();
    }

}
