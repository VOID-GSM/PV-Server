package pv.domain.assignment.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// 제출/재제출 공용. 텍스트·파일·GitHub 링크 중 하나 이상 필수(서비스에서 검증)
public record SubmissionRequest(
        @Size(max = 10000) String content,
        @Size(max = 10) List<String> fileKeys,
        @Size(max = 500) @Pattern(regexp = "^$|^https://github\\.com/\\S+$", message = "GitHub 링크 형식이 아닙니다.")
        String githubUrl
) {
}
