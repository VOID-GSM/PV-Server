package pv.domain.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicCandidateCreateRequest(@NotBlank @Size(max = 200) String title) {
}