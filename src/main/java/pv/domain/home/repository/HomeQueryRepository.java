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

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HomeQueryRepository {
    private final EntityManager em;

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
}