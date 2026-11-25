
package pv.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;
import pv.domain.notification.dto.request.DiscordRoleUpdateRequest;
import pv.domain.notification.dto.response.DiscordRoleResponse;
import pv.domain.notification.dto.request.DiscordWebhookUpdateRequest;
import pv.domain.notification.entity.DiscordSetting;
import pv.domain.notification.repository.DiscordSettingRepository;
import pv.global.exception.CustomException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import pv.domain.notification.dto.response.DiscordWebhookStatusResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscordSettingService {

    private final MemberRepository memberRepository;
    private final DiscordSettingRepository discordSettingRepository;
    private final GenerationRepository generationRepository;

    // 부장만 설정할 수 있다. 팀에 설정이 없으면 만들고, 있으면 URL을 교체한다.
    @Transactional
    public void updateWebhook(Long loginMemberId, DiscordWebhookUpdateRequest request) {
        Member me = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));

        if (me.getStatus() != MemberStatus.JOINED || !me.isLeader()) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN, "부장만 디스코드 웹훅을 설정할 수 있습니다.");
        }

        discordSettingRepository.findByTeamId(me.getTeamId())
                .ifPresentOrElse(
                        setting -> setting.changeWebhookUrl(request.webhookUrl()),
                        () -> discordSettingRepository.save(
                                new DiscordSetting(me.getTeamId(), request.webhookUrl()))
                );
    }

    // 부장만 설정할 수 있다. 웹훅이 먼저 등록되어 있어야 한다.
    @Transactional
    public void updateRoles(Long loginMemberId, DiscordRoleUpdateRequest request) {
        Member me = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));

        if (me.getStatus() != MemberStatus.JOINED || !me.isLeader()) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN, "부장만 디스코드 역할을 설정할 수 있습니다.");
        }

        validateGenerations(me.getTeamId(), request.generationRoleIds().keySet());

        DiscordSetting setting = discordSettingRepository.findByTeamId(me.getTeamId())
                .orElseThrow(() -> new CustomException(
                        HttpStatus.BAD_REQUEST, "디스코드 웹훅을 먼저 등록해주세요."));

        setting.replaceRoles(request.generationRoleIds(), request.majorRoleIds());
    }

    // 가입 승인된 팀원이면 누구나 조회할 수 있다. 설정이 없으면 빈 값
    public DiscordRoleResponse getRoles(Long loginMemberId) {
        Member me = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));

        if (!me.isJoined()) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있습니다.");
        }

        return discordSettingRepository.findByTeamId(me.getTeamId())
                .map(s -> new DiscordRoleResponse(
                        Map.copyOf(s.getGenerationRoleIds()), Map.copyOf(s.getMajorRoleIds())))
                .orElseGet(() -> new DiscordRoleResponse(Map.of(), Map.of()));
    }

    // 메시지를 만들 때 쓴다. 기수 → 전공 순서로, 역할이 등록된 것만 반환한다.
    public List<String> findRoleIds(Long teamId, Collection<Long> generationIds, Collection<Major> majors) {
        return discordSettingRepository.findByTeamId(teamId)
                .map(s -> {
                    List<String> generationRoles = generationIds.stream()
                            .sorted()
                            .map(s.getGenerationRoleIds()::get)
                            .filter(Objects::nonNull)
                            .toList();
                    List<String> majorRoles = majors.stream()
                            .sorted()
                            .map(s.getMajorRoleIds()::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return java.util.stream.Stream.concat(generationRoles.stream(), majorRoles.stream())
                            .distinct()
                            .toList();
                })
                .orElse(List.of());
    }

    // 같은 팀의 기수만 지정할 수 있다.
    private void validateGenerations(Long teamId, Collection<Long> generationIds) {
        if (generationIds.isEmpty()) {
            return;
        }
        List<Generation> found = generationRepository.findAllById(generationIds);
        boolean valid = found.size() == generationIds.size()
                && found.stream().allMatch(g -> g.getTeamId().equals(teamId));
        if (!valid) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "존재하지 않는 기수입니다.");
        }
    }

    // 발송할 때 쓴다. 설정이 없으면 비어 있다.
    public Optional<String> findWebhookUrl(Long teamId) {
        return discordSettingRepository.findByTeamId(teamId)
                .map(DiscordSetting::getWebhookUrl);
    }

    // 가입 승인된 팀원이면 누구나 등록 여부만 조회할 수 있다.
    public DiscordWebhookStatusResponse getStatus(Long loginMemberId) {
        Member me = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));

        if (!me.isJoined()) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있습니다.");
        }

        return new DiscordWebhookStatusResponse(
                discordSettingRepository.findByTeamId(me.getTeamId()).isPresent());
    }
}
