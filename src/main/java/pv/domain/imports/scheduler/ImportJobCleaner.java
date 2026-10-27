package pv.domain.imports.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.repository.ImportJobRepository;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportJobCleaner {

    private final ImportJobRepository jobRepository;
    private final Clock clock;

    // 서버 재시작 등으로 PROCESSING에 멈춘 job을 FAILED로 정리 (10분마다)
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    @Transactional
    public void failStaleJobs() {
        LocalDateTime threshold = LocalDateTime.now(clock).minus(ImportJob.STALE_AFTER);
        int count = jobRepository.failStaleJobs(threshold);
        if (count > 0) {
            log.warn("멈춘 가져오기 {}건을 FAILED로 변경했습니다.", count);
        }
    }
}