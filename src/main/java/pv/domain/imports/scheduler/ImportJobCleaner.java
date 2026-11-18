package pv.domain.imports.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.repository.ImportDocumentRepository;
import pv.domain.imports.repository.ImportJobRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportJobCleaner {

    private static final int EXPIRED_BATCH_SIZE = 100;

    private final ImportJobRepository jobRepository;
    private final ImportDocumentRepository documentRepository;
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

    // 확정하지 않고 오래 둔 READY job과 임시 문서를 삭제 (1시간마다)
    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 2 * 60 * 1000L)
    @Transactional
    public void deleteExpiredReadyJobs() {
        LocalDateTime threshold = LocalDateTime.now(clock).minus(ImportJob.READY_EXPIRE_AFTER);
        List<ImportJob> expired = jobRepository.findExpiredReadyForUpdate(
                threshold, PageRequest.of(0, EXPIRED_BATCH_SIZE));
        if (expired.isEmpty()) {
            return;
        }
        List<Long> ids = expired.stream().map(ImportJob::getId).toList();
        documentRepository.deleteAllByImportIdIn(ids); // 임시 문서 먼저 삭제
        jobRepository.deleteAllInBatch(expired);
        log.info("만료된 가져오기 {}건을 삭제했습니다.", ids.size());
    }
}