package pv.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.board.entity.Deadline;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    List<Deadline> findAllByBoardIdOrderByDeadlineDateAscIdAsc(Long boardId);

    Optional<Deadline> findByIdAndBoardId(Long id, Long boardId);

    List<Deadline> findAllByAssigneeIdAndDeadlineDateIn(Long assigneeId, Collection<LocalDate> dates);

    void deleteAllByBoardId(Long boardId);
}
