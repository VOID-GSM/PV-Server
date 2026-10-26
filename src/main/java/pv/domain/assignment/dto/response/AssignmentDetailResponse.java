package pv.domain.assignment.dto.response;

import pv.domain.assignment.entity.Assignment;
import pv.domain.assignment.entity.AssignmentTargetType;
import pv.domain.member.entity.Major;
import pv.domain.skills.entity.Skill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AssignmentDetailResponse(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        AssignmentTargetType targetType,
        Set<Major> targetMajors,
        Set<Long> targetMemberIds,
        List<String> fileKeys,
        List<SkillItem> skills,
        Long authorId,
        String authorName,
        LocalDateTime createdAt
) {
    public record SkillItem(Long id, String name) {
        public static SkillItem from(Skill s) {
            return new SkillItem(s.getId(), s.getName());
        }
    }

    // skills: 서비스가 in 쿼리 1회로 조회해 정렬까지 마친 목록
    public static AssignmentDetailResponse of(Assignment a, List<Skill> skills) {
        return new AssignmentDetailResponse(
                a.getId(), a.getTitle(), a.getDescription(), a.getDeadline(), a.getTargetType(),
                Set.copyOf(a.getTargetMajors()), Set.copyOf(a.getTargetMemberIds()),
                List.copyOf(a.getFileKeys()),
                skills.stream().map(SkillItem::from).toList(),
                a.getAuthor().getId(), a.getAuthor().getName(), a.getCreatedAt());
    }
}