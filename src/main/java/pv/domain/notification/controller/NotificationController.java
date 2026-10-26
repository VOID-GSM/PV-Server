
package pv.domain.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.notification.dto.request.NotificationSettingUpdateRequest;
import pv.domain.notification.dto.response.NotificationListResponse;
import pv.domain.notification.dto.response.NotificationSettingsResponse;
import pv.domain.notification.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 조회
    @GetMapping
    public ResponseEntity<NotificationListResponse> getMine(
            @AuthenticationPrincipal Long loginMemberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getMine(loginMemberId, cursor, size)
        );
    }

    // 내 알림 설정 전체 조회
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsResponse> getSettings(
            @AuthenticationPrincipal Long loginMemberId) {
        return ResponseEntity.ok(
                notificationService.getSettings(loginMemberId)
        );
    }

    // 알림 읽음 처리
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long notificationId) {
        notificationService.markRead(loginMemberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    // 모든 알림 읽음 처리
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal Long loginMemberId) {
        notificationService.markAllRead(loginMemberId);
        return ResponseEntity.noContent().build();
    }

    // 알림 설정 변경
    @PatchMapping("/settings")
    public ResponseEntity<Void> updateSetting(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody NotificationSettingUpdateRequest request) {
        notificationService.updateSetting(loginMemberId, request);
        return ResponseEntity.noContent().build();
    }
}
