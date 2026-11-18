package pv.domain.retrospectives.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RetrospectiveUpdateRequest(
        @NotBlank(message = "회고 내용을 입력해 주세요.")
        String content
) {
}