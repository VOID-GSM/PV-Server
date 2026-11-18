package pv.domain.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.notification.entity.Notification;
import pv.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 커서(id) 기반 최신순 조회. offset/count 쿼리가 없다.
    List<Notification> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long cursor, Pageable pageable);

    boolean existsByIdAndMemberId(Long id, Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now "
            + "where n.id = :id and n.memberId = :memberId and n.readAt is null")
    int markRead(@Param("id") Long id,
                 @Param("memberId") Long memberId,
                 @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.memberId = :memberId and n.readAt is null")
    int markAllRead(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now "
            + "where n.memberId = :memberId and n.type = :type and n.targetId = :targetId and n.readAt is null")
    int markTargetRead(@Param("memberId") Long memberId,
                       @Param("type") NotificationType type,
                       @Param("targetId") Long targetId,
                       @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("delete from Notification n where n.type = :type and n.targetId = :targetId")
    void deleteByTarget(@Param("type") NotificationType type, @Param("targetId") Long targetId);

    // 대상자 전원의 알림을 한 번의 쿼리로 만든다. (IDENTITY라 saveAll은 건건이 insert된다)
    // 해당 종류를 끈 사람(notification_setting.enabled = false)은 제외한다. 반환값은 실제로 만들어진 알림 수.
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into notification (member_id, notification_type, title, target_id, created_at)
            select m.id, :type, :title, :targetId, :createdAt
            from member m
            where m.id in (:memberIds)
              and not exists (
                  select 1 from notification_setting s
                  where s.member_id = m.id
                    and s.notification_type = :type
                    and s.enabled = false)
            """, nativeQuery = true)
    int insertForMembers(@Param("type") String type,
                         @Param("title") String title,
                         @Param("targetId") Long targetId,
                         @Param("createdAt") LocalDateTime createdAt,
                         @Param("memberIds") Collection<Long> memberIds);
}
