package pv.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.assignment.dto.projection.SkillPassCountRow;
import pv.domain.assignment.repository.SkillPassQueryRepository;
import pv.domain.skills.service.AssignmentPassReader;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentPassReaderImpl implements AssignmentPassReader {
    private final SkillPassQueryRepository skillPassQueryRepository;

    @Override
    public Map<Long, Long> countPassedBySkill(Long memberId, Collection<Long> skillIds) {
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        return skillPassQueryRepository.countPassedBySkill(memberId, skillIds).stream()
                .collect(Collectors.toMap(SkillPassCountRow::skillId, SkillPassCountRow::count));
    }
}