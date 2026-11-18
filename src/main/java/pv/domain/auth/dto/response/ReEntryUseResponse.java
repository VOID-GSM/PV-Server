package pv.domain.auth.dto.response;

import pv.domain.member.entity.MemberStatus;

public record ReEntryUseResponse(Long memberId, MemberStatus status, String accessToken, String refreshToken) {}