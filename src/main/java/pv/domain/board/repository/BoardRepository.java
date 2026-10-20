package pv.domain.board.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.board.entity.Board;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findAllByTeamIdOrderByEndDateAsc(Long teamId);

    // 보드 종료/마감 변경/삭제가 동시에 일어나도 순서대로 처리되도록 행 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Board> findWithLockById(Long id);

    @Query("select b.id from Board b where b.teamId = :teamId")
    List<Long> findIdsByTeamId(@Param("teamId") Long teamId);
}
