package pv.domain.notification.event;

public record DiscordNoticeEvent(Long teamId, String message) {
}