package pv.domain.notice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.notice.entity.Notice;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 수정/삭제/재알림이 서로 겹치지 않게 공지 행을 잠그고 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notice n where n.id = :id")
    Optional<Notice> findWithLockById(@Param("id") Long id);
}
