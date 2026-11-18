package pv.domain.calendar.port;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.domain.activity.repository.ActivityMemberRepository;
import pv.domain.activity.repository.ActivityRepository;
import pv.domain.board.repository.BoardRepository;
import pv.domain.member.repository.MemberRepository;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CalendarMembershipAdapter implements CalendarMembershipPort {
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ActivityMemberRepository activityMemberRepository;
    private final ActivityRepository activityRepository;

    @Override
    public Set<Long> boardIdsOf(Long memberId) {
        return memberRepository.findById(memberId)
                .map(m -> new HashSet<>(boardRepository.findIdsByTeamId(m.getTeamId())))
                .orElseGet(HashSet::new);
    }

    @Override
    public Set<Long> activityIdsOf(Long memberId) {
        return new HashSet<>(activityMemberRepository.findActivityIdsByMemberId(memberId));
    }

    @Override
    public Set<Long> teamActivityIdsOf(Long memberId) {
        return memberRepository.findById(memberId)
                .map(m -> new HashSet<>(activityRepository.findIdsByTeamId(m.getTeamId())))
                .orElseGet(HashSet::new);
    }
}