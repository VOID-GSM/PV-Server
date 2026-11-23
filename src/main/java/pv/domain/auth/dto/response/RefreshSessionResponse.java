package pv.domain.auth.dto.response;

import pv.domain.member.entity.MemberStatus;

public record RefreshSessionResponse(
        String accessToken,
        String refreshToken,
        MemberStatus status   // JOINED / PENDING
) {}