package pv.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProfileImageUpdateRequest(@NotNull Long fileId) {
}