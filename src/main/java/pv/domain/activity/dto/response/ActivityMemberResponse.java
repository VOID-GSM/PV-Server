package pv.domain.activity.dto.response;

import pv.domain.activity.entity.ActivityMember;
import pv.domain.activity.entity.ActivityRole;

import java.time.LocalDateTime;

public record ActivityMemberResponse(
        Long memberId, String name, ActivityRole role, String roleLabel, LocalDateTime joinedAt
) {
    public static ActivityMemberResponse of(ActivityMember m, String name) {
        return new ActivityMemberResponse(m.getMemberId(), name, m.getRole(), m.getRole().getLabel(), m.getJoinedAt());
    }
}