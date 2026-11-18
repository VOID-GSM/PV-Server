package pv.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;
import pv.domain.member.entity.Role;

public record RoleGrantRequest(@NotNull Role role) {}