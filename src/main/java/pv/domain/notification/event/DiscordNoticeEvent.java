package pv.domain.notification.event;

import java.util.Set;

// 디스코드로 보낼 메시지. 실제로 알림을 울릴 대상(everyone / 역할 / 사용자)만 명시해서 담는다.
public record DiscordNoticeEvent(
        Long teamId,
        String message,
        boolean mentionEveryone,
        Set<String> roleIds,
        Set<String> userIds
) {
    public DiscordNoticeEvent {
        roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
    }

    // 멘션 없이 보내는 메시지
    public DiscordNoticeEvent(Long teamId, String message) {
        this(teamId, message, false, Set.of(), Set.of());
    }
}
