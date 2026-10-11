package pv.domain.files.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UploadUrlRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 127) String contentType,
        @NotNull @Positive Long size
) {}
