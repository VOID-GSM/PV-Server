package pv.domain.interviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.interviews.dto.AnswerResponse;
import pv.domain.interviews.entity.InterviewAnswer;

import java.util.Collection;
import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    long countByQuestionId(Long questionId);
    boolean existsByQuestionId(Long questionId);
    boolean existsByQuestionGroupId(Long groupId);
    boolean existsByQuestionIdAndMemberId(Long questionId, Long memberId);

    @Query("""
            select new pv.domain.interviews.dto.AnswerResponse(
                a.id, m.id, m.name, a.content, a.createdAt)
            from InterviewAnswer a
            join a.member m
            where a.question.id = :questionId
            order by a.createdAt
            """)
    List<AnswerResponse> findAllResponses(@Param("questionId") Long questionId);

    @Query("select a.question.id, a.member.id from InterviewAnswer a where a.question.id in :questionIds")
    List<Object[]> findAnsweredPairs(@Param("questionIds") Collection<Long> questionIds);
}
