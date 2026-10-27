package pv.domain.imports.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pv.domain.imports.dto.response.ImportCreateResponse;
import pv.domain.imports.dto.response.ImportStatusResponse;
import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.entity.ImportStatus;
import pv.domain.imports.repository.ImportDocumentRepository;
import pv.domain.imports.repository.ImportJobRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException;
import pv.domain.imports.dto.response.ImportDocumentPreview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

    private final Clock clock;
    private final ImportJobRepository jobRepository;
    private final ImportDocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final ImportProcessor processor;

    // 트랜잭션 없음: job 저장이 먼저 커밋돼야 비동기 처리기가 job을 찾을 수 있음
    public ImportCreateResponse upload(Long memberId, MultipartFile file) {
        validateJoined(memberId);
        validateFile(file);

        Path temp = null;
        try {
            temp = Files.createTempFile("import-", ".zip");
            String hash = copyWithHash(file, temp);
            checkZipMagic(temp);

            ImportJob job = createJob(memberId, hash, safeFileName(file));
            try {
                processor.process(job.getId(), temp);
            } catch (TaskRejectedException e) {
                jobRepository.deleteById(job.getId());
                throw new CustomException(HttpStatus.SERVICE_UNAVAILABLE,
                        "가져오기 요청이 많습니다. 잠시 후 다시 시도해주세요.");
            }
            temp = null; // 임시 파일 삭제 책임이 처리기로 넘어감
            return ImportCreateResponse.from(job);
        } catch (IOException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하는 중 오류가 발생했습니다.");
        } finally {
            if (temp != null) {
                deleteQuietly(temp);
            }
        }
    }

    @Transactional(readOnly = true)
    public ImportStatusResponse get(Long memberId, Long importId) {
        validateJoined(memberId);
        ImportJob job = jobRepository.findByIdAndMemberId(importId, memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "가져오기 기록을 찾을 수 없습니다."));
        List<ImportDocumentPreview> documents = job.getStatus() == ImportStatus.READY
                ? documentRepository.findPreviewsByImportId(job.getId())
                : List.of();
        return ImportStatusResponse.of(job, documents);
    }

    private ImportJob createJob(Long memberId, String hash, String fileName) {
        Optional<ImportJob> existing = jobRepository.findByMemberIdAndContentHash(memberId, hash);
        if (existing.isPresent()) {
            ImportJob old = existing.get();
            boolean retryable = old.getStatus() == ImportStatus.FAILED
                    || old.isStale(LocalDateTime.now(clock)); // 오래 멈춘 PROCESSING도 재시도 허용
            if (!retryable) {
                throw new CustomException(HttpStatus.CONFLICT,
                        "이미 업로드한 파일입니다. (importId=" + old.getId() + ")");
            }
            jobRepository.deleteById(old.getId()); // 실패·멈춘 기록은 지우고 다시 시도
        }
        try {
            return jobRepository.saveAndFlush(new ImportJob(memberId, hash, fileName));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(HttpStatus.CONFLICT, "같은 파일을 이미 업로드했습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일은 최대 50MB까지 업로드할 수 있습니다.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "ZIP 파일만 업로드할 수 있습니다.");
        }
    }

    // 실제로 읽은 바이트 수로 크기를 제한하면서 SHA-256 계산
    private String copyWithHash(MultipartFile file, Path temp) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(temp)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > MAX_UPLOAD_BYTES) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "파일은 최대 50MB까지 업로드할 수 있습니다.");
                }
                digest.update(buffer, 0, n);
                out.write(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    // 확장자만 믿지 않고 ZIP 시그니처("PK\3\4") 확인
    private void checkZipMagic(Path temp) throws IOException {
        try (InputStream in = Files.newInputStream(temp)) {
            byte[] h = in.readNBytes(4);
            if (h.length < 4 || h[0] != 'P' || h[1] != 'K' || h[2] != 3 || h[3] != 4) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "올바른 ZIP 파일이 아닙니다.");
            }
        }
    }

    // 경로가 섞여 들어와도 파일명만 사용
    private String safeFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "upload.zip";
        }
        int idx = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String base = idx >= 0 ? name.substring(idx + 1) : name;
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }

    private void validateJoined(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원이 없습니다."));
        if (member.getStatus() != MemberStatus.JOINED) {
            throw new CustomException(HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있습니다.");
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 삭제 실패는 무시
        }
    }
}