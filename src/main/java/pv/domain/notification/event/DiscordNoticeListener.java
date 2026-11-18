package pv.domain.notification.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pv.domain.notification.client.DiscordWebhookClient;
import pv.domain.notification.service.DiscordSettingService;

@Component
@RequiredArgsConstructor
public class DiscordNoticeListener {

    private final DiscordSettingService discordSettingService;
    private final DiscordWebhookClient client;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DiscordNoticeEvent event) {
        discordSettingService.findWebhookUrl(event.teamId())
                .ifPresent(url -> client.send(url, event.message()));
    }
}