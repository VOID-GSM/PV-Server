package pv.domain.imports.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.imports.entity.ImportJob;

import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    Optional<ImportJob> findByIdAndMemberId(Long id, Long memberId);

    Optional<ImportJob> findByMemberIdAndContentHash(Long memberId, String contentHash);

    // 확정용: 행 잠금으로 동시 확정 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from ImportJob j where j.id = :id and j.memberId = :memberId")
    Optional<ImportJob> findByIdAndMemberIdForUpdate(@Param("id") Long id, @Param("memberId") Long memberId);
}