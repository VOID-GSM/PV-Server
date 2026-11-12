package pv.domain.home.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pv.domain.activity.entity.ActivityRole;
import pv.domain.activity.entity.SessionStatus;
import pv.domain.activity.entity.TopicMode;
import pv.domain.board.entity.BoardStatus;
import pv.domain.attendance.dto.StatusCount;
import pv.domain.attendance.entity.AttendanceStatus;
import pv.domain.member.entity.MemberStatus;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HomeQueryRepository {
    private final EntityManager em;

    private static final String ASSIGNMENT_TARGETS_CTE = """
        WITH targets AS (
            SELECT assignment_id, member_id FROM assignment_recipients
        )
        """;

    public List<SessionTodoRow> findTopicInputCandidates(Long memberId, LocalDate today) {
        return em.createQuery("""
                        select new pv.domain.home.repository.SessionTodoRow(
                            s.id, a.name, s.sessionDate, a.topicOpenDaysBefore)
                        from ActivitySession s, Activity a, ActivityMember am
                        where a.id = s.activityId
                          and am.activityId = a.id
                          and am.memberId = :memberId
                          and am.role = :leader
                          and a.topicMode = :assigned
                          and s.status = :scheduled
                          and s.topic is null
                          and s.sessionDate >= :today
                        order by s.sessionDate
                        """, SessionTodoRow.class)
                .setParameter("memberId", memberId)
                .setParameter("leader", ActivityRole.LEADER)
                .setParameter("assigned", TopicMode.ASSIGNED)
                .setParameter("scheduled", SessionStatus.SCHEDULED)
                .setParameter("today", today)
                .getResultList();
    }

    public List<SessionTodoRow> findVoteCandidates(Long memberId, LocalDate today) {
        return em.createQuery("""
                        select new pv.domain.home.repository.SessionTodoRow(
                            s.id, a.name, s.sessionDate, a.topicOpenDaysBefore)
                        from ActivitySession s, Activity a, ActivityMember am
                        where a.id = s.activityId
                          and am.activityId = a.id
                          and am.memberId = :memberId
                          and a.topicMode = :vote
                          and s.status = :scheduled
                          and s.topic is null
                          and s.sessionDate >= :today
                          and not exists (
                              select 1 from TopicVote v
                              where v.sessionId = s.id and v.memberId = :memberId)
                        order by s.sessionDate
                        """, SessionTodoRow.class)
                .setParameter("memberId", memberId)
                .setParameter("vote", TopicMode.VOTE)
                .setParameter("scheduled", SessionStatus.SCHEDULED)
                .setParameter("today", today)
                .getResultList();
    }

    /**
     * 내가 대상인 과제 중 미제출 또는 재제출 요청 상태인 것.
     * 마감이 지난 과제도 overdueFrom 이후 것은 지각 제출 유도를 위해 포함한다.
     */
    @SuppressWarnings("unchecked")
    public List<AssignmentTodoRow> findAssignmentTodos(Long memberId, LocalDateTime overdueFrom) {
        List<Object[]> rows = em.createNativeQuery(ASSIGNMENT_TARGETS_CTE + """
                        SELECT a.id, a.title, a.deadline, sub.status
                        FROM targets t
                        JOIN assignments a ON a.id = t.assignment_id
                        LEFT JOIN submissions sub
                               ON sub.assignment_id = a.id AND sub.submitter_id = t.member_id
                        WHERE t.member_id = :memberId
                          AND (sub.id IS NULL OR sub.status = 'RESUBMIT_REQUESTED')
                          AND a.deadline >= :overdueFrom
                        ORDER BY a.deadline
                        """)
                .setParameter("memberId", memberId)
                .setParameter("overdueFrom", overdueFrom)
                .getResultList();

        return rows.stream()
                .map(r -> new AssignmentTodoRow(
                        toLong(r[0]),
                        (String) r[1],
                        toLocalDateTime(r[2]),
                        (String) r[3]))
                .toList();
    }

    /** 나에게 온 피드백 요청 중 아직 완료되지 않은 것 (completed_at IS NULL). */
    @SuppressWarnings("unchecked")
    public List<FeedbackTodoRow> findFeedbackTodos(Long memberId) {
        List<Object[]> rows = em.createNativeQuery("""
                        SELECT fr.id, fr.target_type, fr.target_id, m.name, fr.message
                        FROM feedback_requests fr
                        JOIN member m ON m.id = fr.requester_id
                        WHERE fr.receiver_id = :memberId
                          AND fr.completed_at IS NULL
                        ORDER BY fr.created_at
                        """)
                .setParameter("memberId", memberId)
                .getResultList();

        return rows.stream()
                .map(r -> new FeedbackTodoRow(
                        toLong(r[0]),
                        (String) r[1],
                        toLong(r[2]),
                        (String) r[3],
                        (String) r[4]))
                .toList();
    }

    /** 팀원별로 마감이 지난 대상 과제 수와 그중 제출한 수. */
    @SuppressWarnings("unchecked")
    public List<AssignmentRateRow> countAssignmentRateByMember(Long teamId, LocalDateTime now) {
        List<Object[]> rows = em.createNativeQuery(ASSIGNMENT_TARGETS_CTE + """
                        SELECT t.member_id, COUNT(*), COUNT(sub.id)
                        FROM targets t
                        JOIN assignments a ON a.id = t.assignment_id
                        LEFT JOIN submissions sub
                               ON sub.assignment_id = a.id AND sub.submitter_id = t.member_id
                        WHERE a.team_id = :teamId
                          AND a.deadline < :now
                        GROUP BY t.member_id
                        """)
                .setParameter("teamId", teamId)
                .setParameter("now", now)
                .getResultList();

        return rows.stream()
                .map(r -> new AssignmentRateRow(toLong(r[0]), toLong(r[1]), toLong(r[2])))
                .toList();
    }

    public List<DashboardMemberRow> findTeamMembers(Long teamId) {
        return em.createQuery("""
                    select new pv.domain.home.repository.DashboardMemberRow(m.id, m.name, g.number)
                    from Member m left join m.generation g
                    where m.teamId = :teamId and m.status = :joined
                    order by g.number, m.name
                    """, DashboardMemberRow.class)
                .setParameter("teamId", teamId)
                .setParameter("joined", MemberStatus.JOINED)
                .getResultList();
    }

    public List<IdCountRow> countActiveBoardsByAssignee(Long teamId) {
        return em.createQuery("""
                    select new pv.domain.home.repository.IdCountRow(d.assigneeId, count(distinct d.boardId))
                    from Deadline d, Board b
                    where b.id = d.boardId
                      and b.teamId = :teamId
                      and b.status = :active
                      and d.assigneeId is not null
                    group by d.assigneeId
                    """, IdCountRow.class)
                .setParameter("teamId", teamId)
                .setParameter("active", BoardStatus.ACTIVE)
                .getResultList();
    }

    public List<IdCountRow> countWeeklyDeadlinesByAssignee(Long teamId, LocalDate weekStart, LocalDate weekEnd) {
        return em.createQuery("""
                    select new pv.domain.home.repository.IdCountRow(d.assigneeId, count(d))
                    from Deadline d, Board b
                    where b.id = d.boardId
                      and b.teamId = :teamId
                      and b.status = :active
                      and d.assigneeId is not null
                      and d.deadlineDate between :weekStart and :weekEnd
                    group by d.assigneeId
                    """, IdCountRow.class)
                .setParameter("teamId", teamId)
                .setParameter("active", BoardStatus.ACTIVE)
                .setParameter("weekStart", weekStart)
                .setParameter("weekEnd", weekEnd)
                .getResultList();
    }

    public List<StatusCount> countAttendanceByMember(Long teamId) {
        return em.createQuery("""
                    select new pv.domain.attendance.dto.StatusCount(a.memberId, a.status, count(a))
                    from Attendance a, Activity act
                    where act.id = a.activityId
                      and act.teamId = :teamId
                      and a.status <> :pending
                    group by a.memberId, a.status
                    """, StatusCount.class)
                .setParameter("teamId", teamId)
                .setParameter("pending", AttendanceStatus.PENDING)
                .getResultList();
    }

    // native query 결과 타입은 Hibernate 버전·드라이버에 따라 달라질 수 있어 방어적으로 변환한다
    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}