package pv.domain.posts.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @Size(max = 200)
        String title,
        @Size(max = 50000)
        String content,
        @Size(max = 10, message = "태그는 최대 10개까지 등록할 수 있습니다.")
        List<@NotBlank @Size(max = 30, message = "태그는 30자 이하여야 합니다.") String> tags
) {
    @AssertTrue(message = "수정할 항목이 없습니다.")
    public boolean isNotEmpty() {
        return title != null || content != null || tags != null;
    }

    @AssertTrue(message = "제목과 본문은 빈 값으로 바꿀 수 없습니다.")
    public boolean isNotBlankIfPresent() {
        return (title == null || !title.isBlank()) && (content == null || !content.isBlank());
    }
}
