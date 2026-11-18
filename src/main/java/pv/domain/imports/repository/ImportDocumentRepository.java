package pv.domain.imports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.imports.entity.ImportDocument;

import java.util.List;
import java.util.Collection;
import pv.domain.imports.dto.response.ImportDocumentPreview;

public interface ImportDocumentRepository extends JpaRepository<ImportDocument, Long> {

    List<ImportDocument> findAllByImportIdOrderByIdAsc(Long importId);

    @Modifying
    @Query("delete from ImportDocument d where d.importId = :importId")
    void deleteAllByImportId(@Param("importId") Long importId);

    @Modifying
    @Query("delete from ImportDocument d where d.importId in :importIds")
    void deleteAllByImportIdIn(@Param("importIds") Collection<Long> importIds);

    // 미리보기용: 본문 전체 대신 앞 200자와 길이만 조회
    @Query("""
            select new pv.domain.imports.dto.response.ImportDocumentPreview(
                d.id, d.sourcePath, d.title, substring(d.content, 1, 200), length(d.content))
            from ImportDocument d
            where d.importId = :importId
            order by d.id
            """)
    List<ImportDocumentPreview> findPreviewsByImportId(@Param("importId") Long importId);
}