package pv.domain.imports.dto.response;

public record ImportConfirmResponse(
        Long importId,
        int postCount,
        int worklogCount,
        int interviewCount,
        int skippedCount,
        int unmatchedCount
) {
}