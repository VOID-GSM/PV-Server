package pv.domain.interviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    @Query("""
            select q from InterviewQuestion q
            where (:major is null or q.major = :major or q.major is null)
              and (:category is null or q.category = :category)
            order by q.id desc
            """)
    List<InterviewQuestion> search(@Param("major") InterviewQuestion.Major major, @Param("category") InterviewCategory category);

    @Query("""
            select q from InterviewQuestion q
            where (:major is null or q.major = :major or q.major is null)
              and (:category is null or q.category = :category)
            order by function('random')
            """)
    List<InterviewQuestion> findRandom(@Param("major") InterviewQuestion.Major major, @Param("category") InterviewCategory category,
                                       Pageable limit);

    Optional<InterviewQuestion> findByContent(String content);
}
