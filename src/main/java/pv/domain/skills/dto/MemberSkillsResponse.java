package pv.domain.skills.dto;

import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record MemberSkillsResponse(
        Long memberId,
        String name,
        Integer generation,
        Set<Major> majors,
        List<MemberSkillItemResponse> skills
) {
    public static MemberSkillsResponse of(Member member, List<MemberSkillItemResponse> skills) {
        return new MemberSkillsResponse(
                member.getId(),
                member.getName(),
                member.getGeneration() == null ? null : member.getGeneration().getNumber(),
                new TreeSet<>(member.getMajors()),
                skills
        );
    }
}