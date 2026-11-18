package pv.domain.imports.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(
        name = "import_document",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_import_document_import_path",
                columnNames = {"import_id", "source_path"}),
        indexes = @Index(name = "idx_import_document_import", columnList = "import_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    private Long importId;

    // ZIP 안의 상대 경로
    @Column(name = "source_path", nullable = false, length = 500)
    private String sourcePath;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public ImportDocument(Long importId, String sourcePath, String title, String content) {
        this.importId = Objects.requireNonNull(importId);
        this.sourcePath = Objects.requireNonNull(sourcePath);
        this.title = Objects.requireNonNull(title);
        this.content = Objects.requireNonNull(content);
    }
}