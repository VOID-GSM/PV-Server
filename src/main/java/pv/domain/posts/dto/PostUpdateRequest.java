package pv.domain.posts.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
        @Size(max = 200)
        String title,
        @Size(max = 50000)
        String content
) {
    @AssertTrue(message = "수정할 항목이 없습니다.")
    public boolean isNotEmpty() {
        return title != null || content != null;
    }

    @AssertTrue(message = "제목과 본문은 빈 값으로 바꿀 수 없습니다.")
    public boolean isNotBlankIfPresent() {
        return (title == null || !title.isBlank()) && (content == null || !content.isBlank());
    }
}
