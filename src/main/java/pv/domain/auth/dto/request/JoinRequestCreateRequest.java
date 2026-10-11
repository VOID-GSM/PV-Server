package pv.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pv.domain.member.entity.Major;

import java.util.Set;

public record JoinRequestCreateRequest(
        @NotBlank String name,
        @NotNull Long generationId,
        @NotEmpty Set<Major> majors) {}