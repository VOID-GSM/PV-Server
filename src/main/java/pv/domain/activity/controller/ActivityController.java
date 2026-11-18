package pv.domain.activity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.activity.dto.request.*;
import pv.domain.activity.dto.response.*;
import pv.domain.activity.entity.ActivityStatus;
import pv.domain.activity.entity.ActivityType;
import pv.domain.activity.service.ActivityService;
import pv.domain.member.entity.Major;

import java.util.List;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    // ===== 활동 =====

    @GetMapping
    public ResponseEntity<List<ActivitySummaryResponse>> getActivities(
            @AuthenticationPrincipal Long loginMemberId,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) Major major,
            @RequestParam(required = false) Long generationId,
            @RequestParam(required = false) ActivityStatus status) {
        return ResponseEntity.ok(activityService.getActivities(loginMemberId, type, major, generationId, status));
    }

    @PostMapping
    public ResponseEntity<ActivitySummaryResponse> createActivity(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody ActivityCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createActivity(loginMemberId, request));
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityDetailResponse> getActivity(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        return ResponseEntity.ok(activityService.getActivity(loginMemberId, activityId));
    }

    @PatchMapping("/{activityId}")
    public ResponseEntity<Void> updateActivity(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityUpdateRequest request) {
        activityService.updateActivity(loginMemberId, activityId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        activityService.deleteActivity(loginMemberId, activityId);
        return ResponseEntity.noContent().build();
    }

    // ===== 참여 신청 =====

    @PostMapping("/{activityId}/applications")
    public ResponseEntity<ApplicationResponse> apply(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.apply(loginMemberId, activityId));
    }

    @GetMapping("/{activityId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplications(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        return ResponseEntity.ok(activityService.getApplications(loginMemberId, activityId));
    }

    @DeleteMapping("/{activityId}/applications/me")
    public ResponseEntity<Void> cancelMyApplication(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        activityService.cancelMyApplication(loginMemberId, activityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{activityId}/applications/{applicationId}/approve")
    public ResponseEntity<Void> approveApplication(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long applicationId) {
        activityService.approveApplication(loginMemberId, activityId, applicationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{activityId}/applications/{applicationId}/reject")
    public ResponseEntity<Void> rejectApplication(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long applicationId) {
        activityService.rejectApplication(loginMemberId, activityId, applicationId);
        return ResponseEntity.noContent().build();
    }

    // ===== 참여자 =====

    @GetMapping("/{activityId}/members")
    public ResponseEntity<List<ActivityMemberResponse>> getMembers(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        return ResponseEntity.ok(activityService.getMembers(loginMemberId, activityId));
    }

    @PatchMapping("/{activityId}/members/{memberId}")
    public ResponseEntity<Void> changeMemberRole(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRoleUpdateRequest request) {
        activityService.changeMemberRole(loginMemberId, activityId, memberId, request);
        return ResponseEntity.noContent().build();
    }

    // ===== 회차 =====

    @GetMapping("/{activityId}/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId) {
        return ResponseEntity.ok(activityService.getSessions(loginMemberId, activityId));
    }

    @GetMapping("/{activityId}/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(activityService.getSession(loginMemberId, activityId, sessionId));
    }

    @PatchMapping("/{activityId}/sessions/{sessionId}")
    public ResponseEntity<Void> updateSession(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionUpdateRequest request) {
        activityService.updateSession(loginMemberId, activityId, sessionId, request);
        return ResponseEntity.noContent().build();
    }

    // ===== 주제 후보 / 투표 =====

    @GetMapping("/{activityId}/sessions/{sessionId}/topic-candidates")
    public ResponseEntity<List<TopicCandidateResponse>> getCandidates(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(activityService.getCandidates(loginMemberId, activityId, sessionId));
    }

    @PostMapping("/{activityId}/sessions/{sessionId}/topic-candidates")
    public ResponseEntity<TopicCandidateResponse> proposeCandidate(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long sessionId,
            @Valid @RequestBody TopicCandidateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.proposeCandidate(loginMemberId, activityId, sessionId, request));
    }

    @PostMapping("/{activityId}/sessions/{sessionId}/topic-candidates/{candidateId}/votes")
    public ResponseEntity<Void> vote(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long sessionId,
            @PathVariable Long candidateId) {
        activityService.vote(loginMemberId, activityId, sessionId, candidateId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{activityId}/sessions/{sessionId}/topic-candidates/{candidateId}/votes/me")
    public ResponseEntity<Void> cancelVote(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long activityId,
            @PathVariable Long sessionId,
            @PathVariable Long candidateId) {
        activityService.cancelVote(loginMemberId, activityId, sessionId, candidateId);
        return ResponseEntity.noContent().build();
    }
}