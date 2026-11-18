package pv.domain.files.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {
    private static final Duration RETENTION = Duration.ofDays(1);
    private static final int BATCH_SIZE = 500;
    private static final int MAX_BATCHES = 100;

    private final FileService fileService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteUnattachedFiles() {
        Instant threshold = Instant.now().minus(RETENTION);
        int total = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            int deleted = fileService.deleteUnattachedCreatedBefore(threshold, BATCH_SIZE);
            total += deleted;
            if (deleted < BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("미첨부 파일 {}건 정리 완료", total);
        }
    }
}
