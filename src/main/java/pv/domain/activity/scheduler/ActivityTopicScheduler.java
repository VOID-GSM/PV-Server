package pv.domain.activity.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pv.domain.activity.service.ActivityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityTopicScheduler {

    private final ActivityService activityService;

    // 매일 00:05, 투표가 마감된 회차의 주제를 확정
    @Scheduled(cron = "0 5 0 * * *")
    public void confirmClosedTopics() {
        int confirmed = activityService.confirmClosedTopics();
        log.info("주제 자동 확정: {}건", confirmed);
    }
}