package pv.domain.files.dto;

import java.time.Instant;
import java.util.Map;

public record UploadUrlResponse(
        String objectKey,
        String uploadUrl,
        Map<String, String> headers,
        Instant expiresAt
) {
}
