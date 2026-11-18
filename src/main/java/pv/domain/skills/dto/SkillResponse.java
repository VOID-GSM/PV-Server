package pv.domain.skills.dto;

import pv.domain.member.entity.Major;
import pv.domain.skills.entity.Skill;

public record SkillResponse(
        Long id,
        Major major,
        String name,
        String description,
        int sortOrder,
        boolean active
) {
    public static SkillResponse from(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getMajor(),
                skill.getName(),
                skill.getDescription(),
                skill.getSortOrder(),
                skill.isActive()
        );
    }
}