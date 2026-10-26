package pv.domain.imports.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pv.domain.imports.entity.ImportDocument;
import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.entity.ImportStatus;
import pv.domain.imports.repository.ImportDocumentRepository;
import pv.domain.imports.repository.ImportJobRepository;
import pv.global.exception.CustomException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportProcessor {

    private final NotionZipParser parser;
    private final ImportJobRepository jobRepository;
    private final ImportDocumentRepository documentRepository;
    private final TransactionTemplate transactionTemplate;

    @Async("importExecutor")
    public void process(Long jobId, Path zipPath) {
        try {
            // 파싱은 트랜잭션 밖에서 (DB 커넥션을 오래 잡지 않기 위해)
            List<NotionZipParser.ParsedDocument> parsed = parser.parse(zipPath);

            // 문서 저장 + READY 변경을 한 트랜잭션으로 (실패 시 전부 롤백)
            transactionTemplate.executeWithoutResult(status -> {
                ImportJob job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new IllegalStateException("가져오기 기록이 없습니다."));
                List<ImportDocument> documents = parsed.stream()
                        .map(d -> new ImportDocument(jobId, d.sourcePath(), d.title(), d.content()))
                        .toList();
                documentRepository.saveAll(documents);
                job.markReady();
            });
        } catch (CustomException e) {
            fail(jobId, e.getMessage());
        } catch (Exception e) {
            log.error("노션 가져오기 처리 실패 importId={}", jobId, e);
            fail(jobId, "처리 중 오류가 발생했습니다.");
        } finally {
            deleteQuietly(zipPath);
        }
    }

    private void fail(Long jobId, String reason) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    jobRepository.findById(jobId).ifPresent(job -> {
                        if (job.getStatus() == ImportStatus.PROCESSING) {
                            job.markFailed(reason);
                        }
                    }));
        } catch (Exception e) {
            log.error("가져오기 실패 상태 기록 실패 importId={}", jobId, e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", path.getFileName());
        }
    }
}