package pv.domain.skills.dto;

import pv.domain.member.entity.Major;
import pv.domain.skills.entity.MemberSkill;
import pv.domain.skills.entity.Skill;

import java.time.LocalDateTime;

public record MemberSkillItemResponse(
        Long skillId,
        Major major,
        String name,
        String description,
        Integer selfScore,
        LocalDateTime assessedAt,
        Long passedAssignmentCount
) {
    public static MemberSkillItemResponse of(Skill skill, MemberSkill assessment, Long passedCount) {
        return new MemberSkillItemResponse(
                skill.getId(),
                skill.getMajor(),
                skill.getName(),
                skill.getDescription(),
                assessment == null ? null : assessment.getScore(),
                assessment == null ? null : assessment.getAssessedAt(),
                passedCount
        );
    }
}