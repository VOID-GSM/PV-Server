package pv.domain.imports.dto.response;

import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.entity.ImportStatus;

public record ImportCreateResponse(Long importId, ImportStatus status) {

    public static ImportCreateResponse from(ImportJob job) {
        return new ImportCreateResponse(job.getId(), job.getStatus());
    }
}