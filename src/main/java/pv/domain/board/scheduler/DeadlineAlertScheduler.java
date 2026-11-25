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

    // 하루 한 번이 아니라 9~21시 매시 실행: 실패한 알림을 다음 시간에 재시도하고,
    // 낮에 새로 만든 오늘·내일 마감도 당일에 알린다. 중복은 deadline_alert_log 유니크 제약이 막는다.
    @Scheduled(cron = "0 0 9-21 * * *", zone = "Asia/Seoul")
    public void sendDailyAlerts() {
        int sent = deadlineAlertService.sendDailyAlerts();
        if (sent > 0) {
            log.info("마감 D-day 알림 발송: {}건", sent);
        }
    }
}
