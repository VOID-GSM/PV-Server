package pv.domain.interviews.dto;

import java.time.LocalDateTime;

public record GroupResponse(
        Long id,
        String name,
        Long createdById,
        String createdByName,
        long questionCount,
        LocalDateTime createdAt
) {
}
