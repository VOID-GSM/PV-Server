package pv.domain.activity.dto.request;

import jakarta.validation.constraints.NotNull;
import pv.domain.activity.entity.ActivityRole;

public record MemberRoleUpdateRequest(@NotNull ActivityRole role) {
}