package pv.domain.interviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pv.domain.interviews.dto.GroupResponse;
import pv.domain.interviews.entity.InterviewGroup;

import java.util.List;

public interface InterviewGroupRepository extends JpaRepository<InterviewGroup, Long> {
    @Query("""
            select new pv.domain.interviews.dto.GroupResponse(g.id, g.name, m.id, m.name, count(q), g.createdAt)            from InterviewGroup g
            join g.createdBy m
            left join InterviewQuestion q on q.group = g
            group by g.id, g.name, m.id, m.name, g.createdAt
            order by g.id desc
            """)
    List<GroupResponse> findAllWithQuestionCount();
}
