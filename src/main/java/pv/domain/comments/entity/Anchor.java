package pv.domain.comments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

@Embeddable
public record Anchor(
        Integer blockIndex,
        Integer startOffset,
        Integer endOffset,
        @Column(columnDefinition = "text") String quotedText
) {
    public static Anchor of(int blockIndex, String blockText, int start, int end) {
        if (blockIndex < 0 || start < 0 || start >= end || end > blockText.length()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "잘못된 앵커 범위");
        }
        return new Anchor(blockIndex, start, end, blockText.substring(start, end));
    }
}
