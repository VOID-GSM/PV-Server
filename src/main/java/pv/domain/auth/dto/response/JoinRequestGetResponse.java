package pv.domain.auth.dto.response;

import pv.domain.member.entity.Major;

import java.time.LocalDateTime;
import java.util.Set;

// requestId = 신청한 멤버의 id (가입 승인 API의 {requestId}로 사용)
public record JoinRequestGetResponse(
        Long requestId, String name, Integer generation, Set<Major> majors, LocalDateTime requestedAt) {}