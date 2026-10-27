package pv.domain.imports.dto.response;

public record ImportDocumentPreview(
        Long documentId,
        String sourcePath,
        String title,
        String preview,
        Integer contentLength
) {
}