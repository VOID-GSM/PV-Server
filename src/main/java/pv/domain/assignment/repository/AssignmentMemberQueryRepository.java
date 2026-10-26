package pv.domain.assignment.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import pv.domain.assignment.dto.projection.TargetMemberRow;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;

import java.util.Collection;
import java.util.List;

// 제출 현황용 대상자 조회 (DTO 직접 조회, 출제자 제외)
public interface AssignmentMemberQueryRepository extends Repository<Member, Long> {

    // ALL 대상: 선배·부장을 뺀 일반 부원
    @Query("""
            select new pv.domain.assignment.dto.projection.TargetMemberRow(m.id, m.name) from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.id <> :excludeId
              and m.id not in (select m2.id from Member m2 join m2.roles r
                               where r in (pv.domain.member.entity.Role.SENIOR,
                                           pv.domain.member.entity.Role.LEADER))
            order by m.name
            """)
    List<TargetMemberRow> findAllJoined(Long teamId, Long excludeId);

    @Query("""
            select new pv.domain.assignment.dto.projection.TargetMemberRow(m.id, m.name) from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.id <> :excludeId
              and m.id in (select m2.id from Member m2 join m2.majors mj where mj in :majors)
            order by m.name
            """)
    List<TargetMemberRow> findJoinedByMajors(Long teamId, Long excludeId, Collection<Major> majors);

    @Query("""
            select new pv.domain.assignment.dto.projection.TargetMemberRow(m.id, m.name) from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.id <> :excludeId
              and m.id in :ids
            order by m.name
            """)
    List<TargetMemberRow> findJoinedByIds(Long teamId, Long excludeId, Collection<Long> ids);
}