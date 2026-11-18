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

    // 1분마다 투표 마감 여부를 확인하고 주제를 자동 확정한다.
    @Scheduled(fixedDelay = 60000)
    public void confirmClosedTopics() {
        int confirmed = activityService.confirmClosedTopics();

        if (confirmed > 0) {
            log.info("주제 자동 확정: {}건", confirmed);
        }
    }
}