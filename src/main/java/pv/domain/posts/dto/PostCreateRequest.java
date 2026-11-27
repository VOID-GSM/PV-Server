package pv.domain.posts.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        Long sessionId,
        Long questionId,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 50000)
        String content,

        @Size(max = 10, message = "태그는 최대 10개까지 등록할 수 있습니다.")
        List<@NotBlank @Size(max = 30, message = "태그는 30자 이하여야 합니다.") String> tags
) {
    @AssertTrue(message = "sessionId 또는 questionId 중 하나는 필요합니다.")
    public boolean isLinked() {
        return sessionId != null || questionId != null;
    }
}
