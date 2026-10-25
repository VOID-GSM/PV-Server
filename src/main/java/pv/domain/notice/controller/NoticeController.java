package pv.domain.notice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.notice.dto.request.NoticeCreateRequest;
import pv.domain.notice.dto.request.NoticeUpdateRequest;
import pv.domain.notice.dto.response.*;
import pv.domain.notice.service.NoticeService;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 내가 대상인 공지 (읽음 여부 포함). 최신순, 커서 기반
    @GetMapping
    public ResponseEntity<NoticeListResponse> getNotices(
            @AuthenticationPrincipal Long loginMemberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(noticeService.getNotices(loginMemberId, cursor, size));
    }

    @PostMapping
    public ResponseEntity<NoticeDetailResponse> create(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noticeService.create(loginMemberId, request));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNotice(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(loginMemberId, noticeId));
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request) {
        noticeService.update(loginMemberId, noticeId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long noticeId) {
        noticeService.delete(loginMemberId, noticeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{noticeId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long noticeId) {
        noticeService.markRead(loginMemberId, noticeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{noticeId}/reads")
    public ResponseEntity<NoticeReadStatusResponse> getReadStatus(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getReadStatus(loginMemberId, noticeId));
    }

    @PostMapping("/{noticeId}/remind")
    public ResponseEntity<NoticeRemindResponse> remind(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.remind(loginMemberId, noticeId));
    }
}
