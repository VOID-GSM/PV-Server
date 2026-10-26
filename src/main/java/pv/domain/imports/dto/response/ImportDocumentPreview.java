package pv.domain.imports.dto.response;

import pv.domain.imports.entity.ImportDocument;

public record ImportDocumentPreview(
        Long documentId,
        String sourcePath,
        String title,
        String preview,
        int contentLength
) {
    private static final int PREVIEW_LENGTH = 200;

    public static ImportDocumentPreview from(ImportDocument d) {
        String content = d.getContent();
        String preview = content.length() > PREVIEW_LENGTH
                ? content.substring(0, PREVIEW_LENGTH)
                : content;
        return new ImportDocumentPreview(d.getId(), d.getSourcePath(), d.getTitle(), preview, content.length());
    }
}