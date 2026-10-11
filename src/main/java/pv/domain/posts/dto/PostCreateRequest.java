package pv.domain.posts.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        Long activityId,
        Long sessionId,
        Long questionId,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 50000)
        String content
) {
    @AssertTrue(message = "sessionId 또는 questionId 중 하나는 필요합니다.")
    public boolean isLinked() {
        return sessionId != null || questionId != null;
    }
}
