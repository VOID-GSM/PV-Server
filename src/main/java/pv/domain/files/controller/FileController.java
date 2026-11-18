package pv.domain.files.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.files.dto.FileDownloadResponse;
import pv.domain.files.dto.FileUploadCompleteRequest;
import pv.domain.files.dto.FileUploadCompleteResponse;
import pv.domain.files.dto.UploadUrlRequest;
import pv.domain.files.dto.UploadUrlResponse;
import pv.domain.files.service.FileService;

import java.net.URI;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> issueUploadUrl(
            @RequestHeader("X-Member-Id") Long memberId, // 로그인 구현 후 교체 예정
            @Valid @RequestBody UploadUrlRequest request) {
        return ResponseEntity.ok(fileService.issueUploadUrl(memberId, request));
    }

    @PostMapping
    public ResponseEntity<FileUploadCompleteResponse> register(
            @RequestHeader("X-Member-Id") Long memberId, // 로그인 구현 후 교체 예정
            @Valid @RequestBody FileUploadCompleteRequest request) {
        FileUploadCompleteResponse response = fileService.register(memberId, request);
        return ResponseEntity.created(URI.create("/files/" + response.fileId())).body(response);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileDownloadResponse> getFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getDownloadUrl(fileId));
    }
}
