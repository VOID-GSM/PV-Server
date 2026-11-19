package pv.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.files.dto.FileDownloadResponse;
import pv.domain.member.dto.request.MemberUpdateRequest;
import pv.domain.member.dto.request.MyProfileUpdateRequest;
import pv.domain.member.dto.request.RoleGrantRequest;
import pv.domain.member.dto.response.MemberGetResponse;
import pv.domain.member.dto.response.MemberProfileImageResponse;
import pv.domain.member.dto.response.RoleGetResponse;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.entity.Role;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import pv.domain.member.dto.request.ProfileImageUpdateRequest;
import pv.domain.files.service.FileService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final GenerationRepository generationRepository;
    private final FileService fileService;

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

    // ===== 프로필 사진 (본인) =====

    @Transactional
    public MemberGetResponse updateProfileImage(Long loginMemberId, ProfileImageUpdateRequest request) {
        Member member = findMember(loginMemberId);
        Long oldFileId = member.getProfileImageFileId();

        if (request.fileId().equals(oldFileId)) {
            return MemberGetResponse.from(member);
        }

        fileService.requireImage(loginMemberId, request.fileId());
        fileService.attachByIds(loginMemberId, List.of(request.fileId()));
        member.changeProfileImage(request.fileId());

        if (oldFileId != null) {
            fileService.deleteByIds(loginMemberId, List.of(oldFileId));
        }
        return MemberGetResponse.from(member);
    }

    @Transactional
    public void deleteProfileImage(Long loginMemberId) {
        Member member = findMember(loginMemberId);
        Long oldFileId = member.getProfileImageFileId();
        if (oldFileId == null) {
            return;
        }
        member.changeProfileImage(null);
        fileService.deleteByIds(loginMemberId, List.of(oldFileId));
    }

    // ===== 프로필 사진 일괄 조회 (같은 팀 멤버) =====

    private static final int MAX_PROFILE_IMAGE_IDS = 100;

    public List<MemberProfileImageResponse> getProfileImages(Long loginMemberId, Set<Long> memberIds) {
        Member me = findMember(loginMemberId);
        if (!me.isJoined()) {
            throw error(HttpStatus.FORBIDDEN, "가입이 완료된 팀원만 사용할 수 있습니다.");
        }
        if (memberIds.size() > MAX_PROFILE_IMAGE_IDS) {
            throw error(HttpStatus.BAD_REQUEST, "한 번에 최대 " + MAX_PROFILE_IMAGE_IDS + "명까지 조회할 수 있습니다.");
        }

        List<Member> members = memberRepository.findAllById(memberIds).stream()
                .filter(m -> me.getTeamId().equals(m.getTeamId()))
                .filter(m -> m.getProfileImageFileId() != null)
                .toList();

        Map<Long, FileDownloadResponse> images = fileService.getDownloadUrls(
                members.stream().map(Member::getProfileImageFileId).toList());

        return members.stream()
                .filter(m -> images.containsKey(m.getProfileImageFileId()))
                .map(m -> new MemberProfileImageResponse(m.getId(), images.get(m.getProfileImageFileId())))
                .toList();
    }
}