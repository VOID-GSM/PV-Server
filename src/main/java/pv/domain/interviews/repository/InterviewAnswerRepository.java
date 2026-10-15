package pv.domain.interviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.interviews.entity.InterviewAnswer;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    long countByQuestionId(Long questionId);

    @Query("""
            select a from InterviewAnswer a join fetch a.member
            where a.question.id = :questionId
            order by a.createdAt
            """)
    List<InterviewAnswer> findAllWithMember(@Param("questionId") Long questionId);
    boolean existsByQuestionIdAndMemberId(Long questionId, Long memberId);
}
