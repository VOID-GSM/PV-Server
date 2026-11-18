package pv.domain.imports.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.imports.entity.ImportJob;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import java.util.List;

import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    Optional<ImportJob> findByIdAndMemberId(Long id, Long memberId);

    Optional<ImportJob> findByMemberIdAndContentHash(Long memberId, String contentHash);

    // 확정용: 행 잠금으로 동시 확정 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from ImportJob j where j.id = :id and j.memberId = :memberId")
    Optional<ImportJob> findByIdAndMemberIdForUpdate(@Param("id") Long id, @Param("memberId") Long memberId);

    // 오래 PROCESSING으로 멈춘 job(서버 재시작 등)을 한 번에 FAILED 처리
    @Modifying(clearAutomatically = true)
    @Query("""
            update ImportJob j
               set j.status = pv.domain.imports.entity.ImportStatus.FAILED,
                   j.failReason = '처리가 중단되었습니다. 다시 업로드해주세요.',
                   j.version = j.version + 1
             where j.status = pv.domain.imports.entity.ImportStatus.PROCESSING
               and j.createdAt < :threshold
            """)
    int failStaleJobs(@Param("threshold") LocalDateTime threshold);

    // 오래된 READY job을 잠그고 조회 (그 사이 확정된 job은 제외됨)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j from ImportJob j
             where j.status = pv.domain.imports.entity.ImportStatus.READY
               and j.createdAt < :threshold
             order by j.id
            """)
    List<ImportJob> findExpiredReadyForUpdate(@Param("threshold") LocalDateTime threshold, Pageable pageable);
}