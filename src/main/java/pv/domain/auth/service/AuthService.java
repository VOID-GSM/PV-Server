package pv.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.auth.dto.request.InviteCreateRequest;
import pv.domain.auth.dto.request.JoinRequestCreateRequest;
import pv.domain.auth.dto.request.TeamSettingsUpdateRequest;
import pv.domain.auth.dto.request.TokenRefreshRequest;
import pv.domain.auth.dto.response.*;
import pv.domain.auth.entity.Invite;
import pv.domain.auth.entity.ReEntryLink;
import pv.domain.auth.entity.RefreshToken;
import pv.domain.auth.entity.Team;
import pv.domain.auth.repository.InviteRepository;
import pv.domain.auth.repository.ReEntryLinkRepository;
import pv.domain.auth.repository.RefreshTokenRepository;
import pv.domain.auth.repository.TeamRepository;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException; // 패키지 경로는 실제 위치에 맞게 수정
import pv.global.security.JwtProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final long REFRESH_TOKEN_DAYS = 30;

    private final InviteRepository inviteRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final GenerationRepository generationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReEntryLinkRepository reEntryLinkRepository;
    private final JwtProvider jwtProvider;

    // ===== 초대 링크 =====

    @Transactional
    public InviteCreateResponse createInvite(Long loginMemberId, Long teamId, InviteCreateRequest request) {
        validateLeader(loginMemberId, teamId);

        LocalDateTime expiresAt = request == null ? null : request.expiresAt();
        Integer maxUses = request == null ? null : request.maxUses();

        Invite invite = inviteRepository.save(new Invite(teamId, generateCode(), expiresAt, maxUses));
        return new InviteCreateResponse(invite.getId(), invite.getCode(), invite.getExpiresAt(), invite.getMaxUses());
    }

    public InviteGetResponse getInvite(String inviteCode) {
        Invite invite = inviteRepository.findByCode(inviteCode)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 초대 링크입니다."));
        validateUsable(invite);

        Team team = findTeam(invite.getTeamId());
        List<InviteGetResponse.GenerationItem> generations = generationRepository
                .findAllByTeamIdOrderByNumberAsc(team.getId())
                .stream()
                .map(g -> new InviteGetResponse.GenerationItem(g.getId(), g.getNumber()))
                .toList();

        return new InviteGetResponse(team.getId(), team.getName(), team.isApprovalRequired(),
                invite.getCode(), generations, List.of(Major.values()));
    }

    @Transactional
    public void deleteInvite(Long loginMemberId, Long teamId, Long inviteId) {
        validateLeader(loginMemberId, teamId);
        Invite invite = inviteRepository.findByIdAndTeamId(inviteId, teamId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 초대 링크입니다."));
        inviteRepository.delete(invite);
    }

    // ===== 가입 =====

    @Transactional
    public JoinRequestCreateResponse createJoinRequest(String inviteCode, JoinRequestCreateRequest request) {
        Invite invite = inviteRepository.findWithLockByCode(inviteCode)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 초대 링크입니다."));
        validateUsable(invite);

        Team team = findTeam(invite.getTeamId());

        // 이 팀의 기수 목록에서만 선택 가능
        Generation generation = generationRepository.findById(request.generationId())
                .filter(g -> g.getTeamId().equals(team.getId()))
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "존재하지 않는 기수입니다."));

        // 승인 기능 OFF -> 바로 입장, ON -> 부장 승인 대기
        MemberStatus status = team.isApprovalRequired() ? MemberStatus.PENDING : MemberStatus.JOINED;

        Member member = memberRepository.save(new Member(
                team.getId(), request.name(), generation, request.majors(), status));

        invite.use(); // 사용 횟수 +1

        TokenRefreshResponse tokens = issueTokens(member.getId());
        return new JoinRequestCreateResponse(member.getId(), status, tokens.accessToken(), tokens.refreshToken());
    }

    public List<JoinRequestGetResponse> getJoinRequests(Long loginMemberId, Long teamId) {
        validateLeader(loginMemberId, teamId);
        return memberRepository.findAllByTeamIdAndStatusOrderByCreatedAtAsc(teamId, MemberStatus.PENDING)
                .stream()
                .map(m -> new JoinRequestGetResponse(
                        m.getId(), m.getName(), m.getGeneration().getNumber(),
                        java.util.Set.copyOf(m.getMajors()), m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void updateSettings(Long loginMemberId, Long teamId, TeamSettingsUpdateRequest request) {
        validateLeader(loginMemberId, teamId);
        findTeam(teamId).changeApprovalRequired(request.approvalRequired());
    }

    @Transactional
    public void approveJoinRequest(Long loginMemberId, Long requestId) {
        Member target = findMember(requestId);
        validateLeader(loginMemberId, target.getTeamId());

        if (target.getStatus() != MemberStatus.PENDING) {
            throw error(HttpStatus.CONFLICT, "승인 대기 상태가 아닙니다.");
        }
        target.approve();
    }

    // ===== 세션 =====

    @Transactional
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        RefreshToken saved = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh 토큰입니다."));

        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw error(HttpStatus.UNAUTHORIZED, "만료된 refresh 토큰입니다.");
        }

        // 토큰 값은 유지하고 만료일만 오늘부터 30일로 연장
        saved.extend(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS));
        return new TokenRefreshResponse(jwtProvider.createAccessToken(saved.getMemberId()), saved.getToken());
    }

    // ===== 재입장 =====

    @Transactional
    public ReEntryLinkCreateResponse createReEntryLink(Long loginMemberId, Long memberId) {
        Member target = findMember(memberId);
        validateLeader(loginMemberId, target.getTeamId());

        // 멤버당 링크 1개 -> 새로 발급하면 본인 확인 API에서도 같은 링크로 바뀜
        ReEntryLink link = reEntryLinkRepository.findByMemberId(memberId).orElse(null);
        if (link == null) {
            link = reEntryLinkRepository.save(new ReEntryLink(memberId, generateCode()));
        } else {
            link.regenerate(generateCode());
        }
        return new ReEntryLinkCreateResponse(memberId, link.getCode());
    }

    @Transactional
    public ReEntryUseResponse useReEntryLink(String reEntryCode) {
        ReEntryLink link = reEntryLinkRepository.findByCode(reEntryCode)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "유효하지 않은 재입장 링크입니다."));
        Member member = findMember(link.getMemberId());
        Team team = findTeam(member.getTeamId());

        // 승인 기능이 켜져 있으면 다시 가입 승인 필요 (부장은 제외)
        member.reEnter(team.isApprovalRequired());

        TokenRefreshResponse tokens = issueTokens(member.getId());
        return new ReEntryUseResponse(member.getId(), member.getStatus(), tokens.accessToken(), tokens.refreshToken());
    }

    @Transactional
    public ReEntryLinkGetResponse getMyReEntryLink(Long loginMemberId) {
        findMember(loginMemberId);

        // 아직 발급된 링크가 없으면 자동 생성
        ReEntryLink link = reEntryLinkRepository.findByMemberId(loginMemberId)
                .orElseGet(() -> reEntryLinkRepository.save(new ReEntryLink(loginMemberId, generateCode())));
        return new ReEntryLinkGetResponse(link.getCode());
    }

    // ===== 공통 =====

    private void validateLeader(Long loginMemberId, Long teamId) {
        Member member = findMember(loginMemberId);
        if (!member.isLeader() || !teamId.equals(member.getTeamId())) {
            throw error(HttpStatus.FORBIDDEN, "부장만 사용할 수 있는 기능입니다.");
        }
    }

    private void validateUsable(Invite invite) {
        if (invite.isExpired()) {
            throw error(HttpStatus.GONE, "만료된 초대 링크입니다.");
        }
        if (invite.isExhausted()) {
            throw error(HttpStatus.GONE, "사용 횟수가 모두 소진된 초대 링크입니다.");
        }
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 팀입니다."));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));
    }

    // access 토큰 + refresh 토큰 발급 (기기마다 새 refresh 토큰을 만들고 기존 기기는 유지)
    private TokenRefreshResponse issueTokens(Long memberId) {
        // 이 멤버의 만료된 토큰만 정리
        refreshTokenRepository.deleteByMemberIdAndExpiresAtBefore(memberId, LocalDateTime.now());

        String refreshToken = generateCode();
        refreshTokenRepository.save(new RefreshToken(
                memberId, refreshToken, LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS)));

        return new TokenRefreshResponse(jwtProvider.createAccessToken(memberId), refreshToken);
    }

    private String generateCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private CustomException error(HttpStatus status, String message) {
        return new CustomException(status, message);
    }
}