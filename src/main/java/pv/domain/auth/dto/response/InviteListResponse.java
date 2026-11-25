package pv.domain.auth.dto.response;

import pv.domain.auth.entity.Invite;

import java.time.LocalDateTime;

// 부장 화면의 초대 링크 목록 한 줄. 만료/소진된 링크도 상태와 함께 내려준다.
public record InviteListResponse(
        Long inviteId,
        String code,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,   // null이면 무기한
        Integer maxUses,           // null이면 무제한
        int useCount,
        boolean expired,
        boolean exhausted
) {
    public static InviteListResponse from(Invite invite) {
        return new InviteListResponse(
                invite.getId(), invite.getCode(), invite.getCreatedAt(), invite.getExpiresAt(),
                invite.getMaxUses(), invite.getUseCount(), invite.isExpired(), invite.isExhausted());
    }
}
