package pv.domain.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.notification.dto.request.DiscordWebhookUpdateRequest;
import pv.domain.notification.dto.request.NotificationSettingUpdateRequest;
import pv.domain.notification.dto.response.NotificationListResponse;
import pv.domain.notification.service.DiscordSettingService;
import pv.domain.notification.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final DiscordSettingService discordSettingService;

    // 최신순, 커서 기반. 첫 요청은 cursor 없이, 다음 요청은 응답의 nextCursor를 넣는다.
    @GetMapping
    public ResponseEntity<NotificationListResponse> getMine(
            @AuthenticationPrincipal Long loginMemberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getMine(loginMemberId, cursor, size));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long notificationId) {
        notificationService.markRead(loginMemberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Long loginMemberId) {
        notificationService.markAllRead(loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/settings")
    public ResponseEntity<Void> updateSetting(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody NotificationSettingUpdateRequest request) {
        notificationService.updateSetting(loginMemberId, request);
        return ResponseEntity.noContent().build();
    }

    // 부장만. 공지를 올릴 디스코드 채널의 웹훅 URL을 등록/교체한다.
    @PatchMapping("/discord")
    public ResponseEntity<Void> updateDiscord(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody DiscordWebhookUpdateRequest request) {
        discordSettingService.updateWebhook(loginMemberId, request);
        return ResponseEntity.noContent().build();
    }
}