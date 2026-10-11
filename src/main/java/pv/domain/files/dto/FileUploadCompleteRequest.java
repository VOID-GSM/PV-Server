package pv.domain.files.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FileUploadCompleteRequest(
        @NotBlank @Size(max = 500) String objectKey
) {
}
