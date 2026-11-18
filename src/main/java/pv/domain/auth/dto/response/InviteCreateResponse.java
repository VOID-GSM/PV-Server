package pv.domain.auth.dto.response;

import java.time.LocalDateTime;

public record InviteCreateResponse(Long inviteId, String code, LocalDateTime expiresAt, Integer maxUses) {}