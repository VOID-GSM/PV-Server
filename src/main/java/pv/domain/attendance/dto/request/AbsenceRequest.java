package pv.domain.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AbsenceRequest(
        @NotNull Long sessionId,
        @NotBlank @Size(max = 500) String reason
) {
}
