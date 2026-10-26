
package pv.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;
import pv.domain.notification.dto.request.DiscordWebhookUpdateRequest;
import pv.domain.notification.entity.DiscordSetting;
import pv.domain.notification.repository.DiscordSettingRepository;
import pv.global.exception.CustomException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscordSettingService {

    private final MemberRepository memberRepository;
    private final DiscordSettingRepository discordSettingRepository;

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

    // 발송할 때 쓴다. 설정이 없으면 비어 있다.
    public Optional<String> findWebhookUrl(Long teamId) {
        return discordSettingRepository.findByTeamId(teamId)
                .map(DiscordSetting::getWebhookUrl);
    }
}
