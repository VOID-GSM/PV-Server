package pv.domain.worklogs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.worklogs.entity.Visibility;
import pv.domain.worklogs.entity.Worklog;

import java.util.Optional;

public interface WorklogRepository extends JpaRepository<Worklog, Long> {
    @EntityGraph(attributePaths = "author")
    @Query("""
            select w from Worklog w
            where (w.visibility = :publicVisibility or w.author.id = :memberId)
                and (:boardId is null or w.board.id = :boardId)
            """)
    Page<Worklog> findVisible(
            @Param("memberId") Long memberId,
            @Param("boardId") Long boardId,
            @Param("publicVisibility") Visibility publicVisibility,
            Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Optional<Worklog> findWithAuthorById(Long id);
}
