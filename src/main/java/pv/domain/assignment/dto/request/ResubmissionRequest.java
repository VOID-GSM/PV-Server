package pv.domain.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResubmissionRequest(@NotBlank @Size(max = 2000) String comment) {
}
