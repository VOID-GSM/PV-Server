package pv.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record TeamSettingsUpdateRequest(@NotNull Boolean approvalRequired) {}