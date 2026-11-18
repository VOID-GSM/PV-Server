package pv.domain.files.storage;

import java.time.Instant;

public record PresignedUrl(String url, Instant expiresAt) {
}
