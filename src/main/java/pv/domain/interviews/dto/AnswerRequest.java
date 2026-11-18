package pv.domain.interviews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.")
        @Size(max = 5000, message = "답변은 최대 5000자까지 입력할 수 있습니다.")
        String content
) {
}
