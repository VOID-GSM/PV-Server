package pv.domain.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AbsenceRejectRequest(
        @NotBlank(message = "반려 사유를 입력해주세요.")
        @Size(max = 100)
        String comment
) {
}
