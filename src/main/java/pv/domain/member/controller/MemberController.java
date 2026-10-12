package pv.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.member.dto.request.MemberUpdateRequest;
import pv.domain.member.dto.request.MyProfileUpdateRequest;
import pv.domain.member.dto.request.RoleGrantRequest;
import pv.domain.member.dto.response.MemberGetResponse;
import pv.domain.member.dto.response.RoleGetResponse;
import pv.domain.member.entity.Role;
import pv.domain.member.service.MemberService;

import java.util.List;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 내 프로필 조회 (본인)
    @GetMapping("/me")
    public ResponseEntity<MemberGetResponse> getMe(@AuthenticationPrincipal Long loginMemberId) {
        return ResponseEntity.ok(memberService.getMe(loginMemberId));
    }

    // 프로필 수정 (본인)
    @PatchMapping("/me")
    public ResponseEntity<MemberGetResponse> updateMe(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody MyProfileUpdateRequest request) {
        return ResponseEntity.ok(memberService.updateMe(loginMemberId, request));
    }

    // 현재 가입된 사람들 조회 (부장)
    @GetMapping
    public ResponseEntity<List<MemberGetResponse>> getMembers(@AuthenticationPrincipal Long loginMemberId) {
        return ResponseEntity.ok(memberService.getMembers(loginMemberId));
    }

    // 특정 사용자 프로필 조회 (부장)
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberGetResponse> getMember(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.getMember(loginMemberId, memberId));
    }

    // 특정 사용자 정보 수정: 기수, 전공 (부장)
    @PostMapping("/{memberId}")
    public ResponseEntity<MemberGetResponse> updateMember(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberUpdateRequest request) {
        return ResponseEntity.ok(memberService.updateMember(loginMemberId, memberId, request));
    }

    // 사용자 역할 조회 (부장)
    @GetMapping("/{memberId}/roles")
    public ResponseEntity<RoleGetResponse> getRoles(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.getRoles(loginMemberId, memberId));
    }

    // 사용자 역할 부여 (부장)
    @PostMapping("/{memberId}/roles")
    public ResponseEntity<RoleGetResponse> grantRole(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId,
            @Valid @RequestBody RoleGrantRequest request) {
        return ResponseEntity.ok(memberService.grantRole(loginMemberId, memberId, request));
    }

    // 사용자 역할 삭제 (부장)
    @DeleteMapping("/{memberId}/roles/{role}")
    public ResponseEntity<Void> removeRole(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId,
            @PathVariable Role role) {
        memberService.removeRole(loginMemberId, memberId, role);
        return ResponseEntity.noContent().build();
    }
}