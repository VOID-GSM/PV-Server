package pv.domain.retrospectives.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.retrospectives.entity.Retrospective;

import java.util.List;
import java.util.Optional;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Retrospective r where r.boardId = :boardId")
    void deleteAllByBoardId(@Param("boardId") Long boardId);

    List<Retrospective> findAllByBoardIdOrderByIdAsc(Long boardId);

    Optional<Retrospective> findByBoardIdAndMemberId(Long boardId, Long memberId);
}