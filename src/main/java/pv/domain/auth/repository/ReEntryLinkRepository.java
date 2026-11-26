package pv.domain.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.auth.entity.ReEntryLink;

import java.util.Optional;

public interface ReEntryLinkRepository extends JpaRepository<ReEntryLink, Long> {

    Optional<ReEntryLink> findByCode(String code);

    Optional<ReEntryLink> findByMemberId(Long memberId);

    // 탈퇴 처리 시 재입장 코드 삭제
    void deleteByMemberId(Long memberId);

    // 재입장 코드는 1회용: 같은 코드로 동시에 요청해도 한 번만 성공하도록 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ReEntryLink l where l.code = :code")
    Optional<ReEntryLink> findWithLockByCode(@Param("code") String code);
}