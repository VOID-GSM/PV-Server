package pv.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByTeamIdAndStatusOrderByCreatedAtAsc(Long teamId, MemberStatus status);
}