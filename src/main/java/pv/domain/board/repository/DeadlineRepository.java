package pv.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.board.entity.Deadline;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    List<Deadline> findAllByBoardIdOrderByDeadlineDateAscIdAsc(Long boardId);

    Optional<Deadline> findByIdAndBoardId(Long id, Long boardId);

    @Query("""
            select distinct d from Deadline d join d.assigneeIds a
            where a in :memberIds and d.deadlineDate in :dates
            """)
    List<Deadline> findAllByAnyAssigneeAndDateIn(@Param("memberIds") Collection<Long> memberIds,
                                                 @Param("dates") Collection<LocalDate> dates);

    List<Deadline> findAllByBoardIdInAndDeadlineDateIn(Collection<Long> boardIds, Collection<LocalDate> dates);

    void deleteAllByBoardId(Long boardId);
}
