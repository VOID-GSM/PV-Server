package pv.domain.skills.service;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class EmptyAssignmentPassReader implements AssignmentPassReader{
    @Override
    public Map<Long, Long> countPassedBySkill(Long memberId, Collection<Long> skillIds) {
        return Map.of();
    }
}