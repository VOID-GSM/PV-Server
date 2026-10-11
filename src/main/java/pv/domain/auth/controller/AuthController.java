package pv.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.auth.dto.request.InviteCreateRequest;
import pv.domain.auth.dto.request.JoinRequestCreateRequest;
import pv.domain.auth.dto.request.TeamSettingsUpdateRequest;
import pv.domain.auth.dto.request.TokenRefreshRequest;
import pv.domain.auth.dto.response.*;
import pv.domain.auth.service.AuthService;

import java.util.List;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 초대 링크 생성 (부장) - body 생략하면 무기한/무제한
    @PostMapping("/teams/{teamId}/invites")
    public ResponseEntity<InviteCreateResponse> createInvite(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long teamId,
            @Valid @RequestBody(required = false) InviteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createInvite(loginMemberId, teamId, request));
    }

    // 초대 링크 확인
    @GetMapping("/invites/{inviteCode}")
    public ResponseEntity<InviteGetResponse> getInvite(@PathVariable String inviteCode) {
        return ResponseEntity.ok(authService.getInvite(inviteCode));
    }

    // 초대 링크 삭제 (부장)
    @DeleteMapping("/teams/{teamId}/invites/{inviteId}")
    public ResponseEntity<Void> deleteInvite(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long teamId,
            @PathVariable Long inviteId) {
        authService.deleteInvite(loginMemberId, teamId, inviteId);
        return ResponseEntity.noContent().build();
    }

    // 프로필 생성 및 가입 신청 (누구나)
    @PostMapping("/invites/{inviteCode}/members")
    public ResponseEntity<JoinRequestCreateResponse> createJoinRequest(
            @PathVariable String inviteCode,
            @Valid @RequestBody JoinRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createJoinRequest(inviteCode, request));
    }

    // 세션 유지 (누구나)
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // 신청 목록 조회 (부장)
    @GetMapping("/teams/{teamId}/join-requests")
    public ResponseEntity<List<JoinRequestGetResponse>> getJoinRequests(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(authService.getJoinRequests(loginMemberId, teamId));
    }

    // 승인 기능 여부 설정 (부장)
    @PatchMapping("/teams/{teamId}/settings")
    public ResponseEntity<Void> updateSettings(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamSettingsUpdateRequest request) {
        authService.updateSettings(loginMemberId, teamId, request);
        return ResponseEntity.noContent().build();
    }

    // 가입 승인 (부장)
    @PatchMapping("/join-requests/{requestId}")
    public ResponseEntity<Void> approveJoinRequest(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long requestId) {
        authService.approveJoinRequest(loginMemberId, requestId);
        return ResponseEntity.noContent().build();
    }

    // 재입장 링크 발급 (부장)
    @PostMapping("/members/{memberId}/re-entry-links")
    public ResponseEntity<ReEntryLinkCreateResponse> createReEntryLink(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createReEntryLink(loginMemberId, memberId));
    }

    // 재입장 링크 사용 (누구나)
    @PostMapping("/re-entry/{reEntryCode}")
    public ResponseEntity<ReEntryUseResponse> useReEntryLink(@PathVariable String reEntryCode) {
        return ResponseEntity.ok(authService.useReEntryLink(reEntryCode));
    }

    // 본인 재입장 링크 확인 (로그인 필요)
    @GetMapping("/members/me/re-entry-link")
    public ResponseEntity<ReEntryLinkGetResponse> getMyReEntryLink(
            @AuthenticationPrincipal Long loginMemberId) {
        return ResponseEntity.ok(authService.getMyReEntryLink(loginMemberId));
    }
}