package pv.domain.interviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.member.entity.Major;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findAllByGroupIdOrderByIdDesc(Long groupId);
    boolean existsByGroupIdAndContent(Long groupId, String content);
    Optional<InterviewQuestion> findByGroupIdAndContent(Long groupId, String content);

    @Query("""
            select q from InterviewQuestion q
            where (:major is null or q.major = :major or q.major is null)
              and (:category is null or q.category = :category)
            order by q.id desc
            """)
    List<InterviewQuestion> search(@Param("major") Major major, @Param("category") InterviewCategory category);

    @Query("""
            select q from InterviewQuestion q
            where (:groupId is null or q.group.id = :groupId)
              and (:major is null or q.major = :major or q.major is null)
              and (:category is null or q.category = :category)
            order by function('random')
            """)
    List<InterviewQuestion> findRandom(@Param("groupId") Long groupId, @Param("major") Major major,
                                       @Param("category") InterviewCategory category, Pageable limit);

    @Modifying(clearAutomatically = true)
    @Query("delete from InterviewQuestion q where q.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") Long groupId);
}
