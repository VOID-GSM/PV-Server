package pv.domain.assignment.dto.request;

import jakarta.validation.constraints.Size;

// 통과 처리용 (코멘트 선택)
public record ReviewRequest(@Size(max = 2000) String comment) {
}
