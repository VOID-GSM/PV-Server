package pv.domain.auth.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

// 둘 다 생략 가능: expiresAt이 null이면 무기한, maxUses가 null이면 무제한
public record InviteCreateRequest(@Future LocalDateTime expiresAt, @Positive Integer maxUses) {}