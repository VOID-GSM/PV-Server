package pv.domain.files.dto;

import java.time.Instant;

public record FileDownloadResponse(
        Long fileId,
        String fileName,
        String contentType,
        Long size,
        String downloadUrl,
        Instant expiresAt
) {
}
