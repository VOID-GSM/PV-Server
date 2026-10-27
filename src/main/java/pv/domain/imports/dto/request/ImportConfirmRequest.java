package pv.domain.imports.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.worklogs.entity.Visibility;

import java.time.LocalDateTime;
import java.util.List;

public record ImportConfirmRequest(
        @NotEmpty(message = "저장할 문서를 1개 이상 선택해주세요.")
        @Size(max = 500, message = "한 번에 최대 500개까지 확정할 수 있습니다.")
        List<@Valid @NotNull(message = "문서 항목이 비어 있습니다.") Item> documents
) {
    public record Item(
            @NotNull(message = "문서 ID는 필수입니다.")
            Long documentId,

            @NotNull(message = "문서 종류는 필수입니다.")
            ImportDocumentType type,

            // 비우면 본인. 부장만 다른 사람 지정 가능
            Long authorId,

            // 정리글에만 적용 (업무일지·면접 질문은 무시)
            Long activityId,

            // 비우면 현재 시각
            LocalDateTime createdAt,

            // 업무일지일 때만 사용, 비우면 PRIVATE
            Visibility visibility
    ) {
    }
}