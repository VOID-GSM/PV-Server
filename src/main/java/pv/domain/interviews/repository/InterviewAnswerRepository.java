package pv.domain.interviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.interviews.dto.AnswerResponse;
import pv.domain.interviews.entity.InterviewAnswer;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    long countByQuestionId(Long questionId);

    @Query("""
        select new pv.domain.interviews.dto.AnswerResponse(
            a.id, m.id, m.name, a.content, a.createdAt)
        from InterviewAnswer a
        join a.member m
        where a.question.id = :questionId
        order by a.createdAt
        """)
    List<AnswerResponse> findAllResponses(@Param("questionId") Long questionId);
    boolean existsByQuestionIdAndMemberId(Long questionId, Long memberId);
    // 이미 답변이 있는 (질문ID, 회원ID) 쌍을 한 번에 조회 (가져오기 중복 방지용)
    @Query("select a.question.id, a.member.id from InterviewAnswer a where a.question.id in :questionIds")
    List<Object[]> findAnsweredPairs(@Param("questionIds") java.util.Collection<Long> questionIds);
}
