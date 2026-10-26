package pv.domain.imports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.imports.entity.ImportDocument;

import java.util.List;

public interface ImportDocumentRepository extends JpaRepository<ImportDocument, Long> {

    List<ImportDocument> findAllByImportIdOrderByIdAsc(Long importId);

    @Modifying
    @Query("delete from ImportDocument d where d.importId = :importId")
    void deleteAllByImportId(@Param("importId") Long importId);
}