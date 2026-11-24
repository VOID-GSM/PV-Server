package pv.domain.board.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pv.domain.board.service.DeadlineAlertService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineAlertScheduler {

    private final DeadlineAlertService deadlineAlertService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyAlerts() {
        int sent = deadlineAlertService.sendDailyAlerts();
        log.info("마감 D-day 알림 발송: {}건", sent);
    }
}