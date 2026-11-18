package pv.domain.imports.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pv.domain.imports.dto.request.ImportConfirmRequest;
import pv.domain.imports.dto.response.ImportConfirmResponse;
import pv.domain.imports.dto.response.ImportCreateResponse;
import pv.domain.imports.dto.response.ImportStatusResponse;
import pv.domain.imports.service.ImportConfirmService;
import pv.domain.imports.service.ImportService;

import java.net.URI;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final ImportConfirmService importConfirmService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportCreateResponse> upload(
            @AuthenticationPrincipal Long memberId,
            @RequestPart("file") MultipartFile file
    ) {
        ImportCreateResponse response = importService.upload(memberId, file);
        return ResponseEntity.accepted()
                .location(URI.create("/import/" + response.importId()))
                .body(response);
    }

    @GetMapping("/{importId}")
    public ImportStatusResponse get(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long importId
    ) {
        return importService.get(memberId, importId);
    }

    @PostMapping("/{importId}/confirm")
    public ImportConfirmResponse confirm(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long importId,
            @RequestBody @Valid ImportConfirmRequest request
    ) {
        return importConfirmService.confirm(memberId, importId, request);
    }
}