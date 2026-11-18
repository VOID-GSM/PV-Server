package pv.domain.member.dto.response;

import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.entity.Role;

import java.util.Set;

public record MemberGetResponse(
        Long memberId, String name, Long generationId, Integer generation,
        Set<Major> majors, Set<Role> roles, MemberStatus status) {

    public static MemberGetResponse from(Member m) {
        return new MemberGetResponse(
                m.getId(), m.getName(),
                m.getGeneration().getId(), m.getGeneration().getNumber(),
                Set.copyOf(m.getMajors()), Set.copyOf(m.getRoles()), m.getStatus());
    }
}