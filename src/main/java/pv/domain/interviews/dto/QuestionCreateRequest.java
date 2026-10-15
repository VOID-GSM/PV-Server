package pv.domain.interviews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;

public record QuestionCreateRequest(
        @NotBlank @Size(max = 500) String content,
        InterviewQuestion.Major major,
        @NotNull InterviewCategory category
        ) {
}
