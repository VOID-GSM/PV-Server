package pv.domain.imports.dto.response;

import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.entity.ImportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ImportStatusResponse(
        Long importId,
        ImportStatus status,
        String fileName,
        String failReason,
        LocalDateTime createdAt,
        List<ImportDocumentPreview> documents
) {
    public static ImportStatusResponse of(ImportJob job, List<ImportDocumentPreview> documents) {
        return new ImportStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getFileName(),
                job.getFailReason(),
                job.getCreatedAt(),
                documents
        );
    }
}