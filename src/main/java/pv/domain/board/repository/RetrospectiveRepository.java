package pv.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.board.entity.Retrospective;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    void deleteAllByBoardId(Long boardId);
}
