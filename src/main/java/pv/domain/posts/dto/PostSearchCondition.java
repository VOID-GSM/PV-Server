package pv.domain.posts.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record PostSearchCondition(
        Long activityId,
        Long sessionId,
        @Pattern(regexp = "me|\\d{1,18}", message = "author는 'me' 또는 숫자여야 합니다.")
        String author,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
        @AssertTrue(message = "시작일은 종료일보다 늦을 수 없습니다.")
        public boolean isValidRange() {
                return from == null || to == null || !from.isAfter(to);
        }
}
