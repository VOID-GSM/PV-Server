package pv.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;

import java.util.Collection;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByTeamIdAndStatusOrderByCreatedAtAsc(Long teamId, MemberStatus status);

    @Query("""
            select m.id from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.id in :ids
            """)
    List<Long> findJoinedIds(Long teamId, Collection<Long> ids);

    @Query("""
            select m.id from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.generation.number < :gen
            """)
    List<Long> findSeniorIds(Long teamId, int gen);

    @Query("""
            select distinct m.id from Member m join m.majors mj
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.generation.number < :gen
              and mj in :majors
            """)
    List<Long> findSeniorIdsByMajors(Long teamId, int gen, Collection<Major> majors);
}