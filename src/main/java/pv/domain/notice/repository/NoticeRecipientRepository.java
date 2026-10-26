package pv.domain.notice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.notice.dto.query.NoticeReadRow;
import pv.domain.notice.dto.query.NoticeSummaryRow;
import pv.domain.notice.entity.NoticeRecipient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {

    Optional<NoticeRecipient> findByNoticeIdAndMemberId(Long noticeId, Long memberId);

    boolean existsByNoticeIdAndMemberId(Long noticeId, Long memberId);

    // 내가 받은 공지 + 내가 작성한 공지 최신순 (커서 기반)
    @Query("""
        select new pv.domain.notice.dto.query.NoticeSummaryRow(
            n.id, n.title, n.scope, n.authorId, a.name, n.createdAt, r.readAt)
        from Notice n
        left join Member a on a.id = n.authorId
        left join NoticeRecipient r on r.noticeId = n.id and r.memberId = :memberId
        where n.id < :cursor
          and (r.memberId = :memberId or n.authorId = :memberId)
        order by n.id desc
        """)
    List<NoticeSummaryRow> findSummaries(@Param("memberId") Long memberId,
                                         @Param("cursor") Long cursor,
                                         Pageable pageable);

    // 읽음 현황: 대상자 이름 + 읽은 시각을 한 번에 조회
    @Query("""
            select new pv.domain.notice.dto.query.NoticeReadRow(r.memberId, m.name, r.readAt)
            from NoticeRecipient r
            left join Member m on m.id = r.memberId
            where r.noticeId = :noticeId
            order by m.name, r.memberId
            """)
    List<NoticeReadRow> findReadRows(@Param("noticeId") Long noticeId);

    @Query("select r.memberId from NoticeRecipient r where r.noticeId = :noticeId and r.readAt is null")
    List<Long> findUnreadMemberIds(@Param("noticeId") Long noticeId);

    // 이미 읽은 경우에는 readAt을 덮어쓰지 않는다. (멱등) 반환값 0이면 이미 읽었거나 대상자가 아님
    @Modifying(flushAutomatically = true)
    @Query("update NoticeRecipient r set r.readAt = :now "
            + "where r.noticeId = :noticeId and r.memberId = :memberId and r.readAt is null")
    int markRead(@Param("noticeId") Long noticeId,
                 @Param("memberId") Long memberId,
                 @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("delete from NoticeRecipient r where r.noticeId = :noticeId")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);

    // 대상자 전원을 한 번의 쿼리로 저장한다. (member 테이블을 기준으로 row를 만들어 존재하는 멤버만 들어간다)
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into notice_recipient (notice_id, member_id)
            select :noticeId, m.id from member m where m.id in (:memberIds)
            """, nativeQuery = true)
    int insertAll(@Param("noticeId") Long noticeId, @Param("memberIds") Collection<Long> memberIds);
}
