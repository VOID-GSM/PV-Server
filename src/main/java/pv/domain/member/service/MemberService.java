package pv.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.member.dto.request.MemberUpdateRequest;
import pv.domain.member.dto.request.MyProfileUpdateRequest;
import pv.domain.member.dto.request.RoleGrantRequest;
import pv.domain.member.dto.response.MemberGetResponse;
import pv.domain.member.dto.response.RoleGetResponse;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.entity.Role;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final GenerationRepository generationRepository;

    // ===== 본인 =====

    public MemberGetResponse getMe(Long loginMemberId) {
        return MemberGetResponse.from(findMember(loginMemberId));
    }

    @Transactional
    public MemberGetResponse updateMe(Long loginMemberId, MyProfileUpdateRequest request) {
        Member member = findMember(loginMemberId);
        member.updateProfile(request.name(), null, request.majors());
        return MemberGetResponse.from(member);
    }

    // ===== 부장 =====

    public List<MemberGetResponse> getMembers(Long loginMemberId) {
        Member leader = getLeader(loginMemberId);
        return memberRepository
                .findAllByTeamIdAndStatusOrderByCreatedAtAsc(leader.getTeamId(), MemberStatus.JOINED)
                .stream()
                .map(MemberGetResponse::from)
                .toList();
    }

    public MemberGetResponse getMember(Long loginMemberId, Long memberId) {
        Member leader = getLeader(loginMemberId);
        return MemberGetResponse.from(findTeamMember(leader, memberId));
    }

    @Transactional
    public MemberGetResponse updateMember(Long loginMemberId, Long memberId, MemberUpdateRequest request) {
        Member leader = getLeader(loginMemberId);
        Member target = findTeamMember(leader, memberId);

        Generation generation = null;
        if (request.generationId() != null) {
            generation = generationRepository.findById(request.generationId())
                    .filter(g -> g.getTeamId().equals(leader.getTeamId()))
                    .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "존재하지 않는 기수입니다."));
        }

        target.updateProfile(request.name(), generation, request.majors());
        return MemberGetResponse.from(target);
    }

    // ===== 역할 =====

    public RoleGetResponse getRoles(Long loginMemberId, Long memberId) {
        Member leader = getLeader(loginMemberId);
        Member target = findTeamMember(leader, memberId);
        return new RoleGetResponse(target.getId(), Set.copyOf(target.getRoles()));
    }

    @Transactional
    public RoleGetResponse grantRole(Long loginMemberId, Long memberId, RoleGrantRequest request) {
        Member leader = getLeader(loginMemberId);
        Member target = findTeamMember(leader, memberId);
        target.grantRole(request.role());
        return new RoleGetResponse(target.getId(), Set.copyOf(target.getRoles()));
    }

    @Transactional
    public void removeRole(Long loginMemberId, Long memberId, Role role) {
        Member leader = getLeader(loginMemberId);
        Member target = findTeamMember(leader, memberId);

        if (!target.hasRole(role)) {
            throw error(HttpStatus.NOT_FOUND, "해당 역할이 없습니다.");
        }
        target.removeRole(role); // 모두 사라지면 MEMBER로 자동 설정
    }

    // ===== 공통 =====

    private Member getLeader(Long loginMemberId) {
        Member member = findMember(loginMemberId);
        if (!member.isLeader()) {
            throw error(HttpStatus.FORBIDDEN, "부장만 사용할 수 있는 기능입니다.");
        }
        return member;
    }

    // 부장과 같은 팀의 멤버만 조회/수정 가능
    private Member findTeamMember(Member leader, Long memberId) {
        Member target = findMember(memberId);
        if (!target.getTeamId().equals(leader.getTeamId())) {
            throw error(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다.");
        }
        return target;
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}