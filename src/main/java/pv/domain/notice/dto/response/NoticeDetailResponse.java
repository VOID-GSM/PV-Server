package pv.domain.notice.dto.response;

import pv.domain.member.entity.Major;
import pv.domain.notice.entity.Notice;
import pv.domain.notice.entity.NoticeScope;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponse(
        Long id,
        NoticeScope scope,
        Long boardId,
        Long activityId,
        List<Long> generationIds,
        List<Major> majors,
        String title,
        String content,
        Long authorId,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean read
) {
    public static NoticeDetailResponse of(Notice n, String authorName, boolean read) {
        // 기수/전공은 TARGET 범위에서만 값이 있다. 다른 범위는 지연 로딩 쿼리를 건드리지 않는다.
        boolean target = n.getScope() == NoticeScope.TARGET;

        return new NoticeDetailResponse(
                n.getId(),
                n.getScope(),
                n.getBoardId(),
                n.getActivityId(),
                target ? n.getTargetGenerationIds().stream().sorted().toList() : List.of(),
                target ? n.getTargetMajors().stream().sorted().toList() : List.of(),
                n.getTitle(),
                n.getContent(),
                n.getAuthorId(),
                authorName,
                n.getCreatedAt(),
                n.getUpdatedAt(),
                read
        );
    }
}
