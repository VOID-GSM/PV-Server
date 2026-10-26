package pv.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pv.domain.files.storage.S3FileStorage;
import pv.global.exception.CustomException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AttachmentValidator {
    private static final int MAX_FILES = 10;
    private final S3FileStorage fileStorage;

    // null → 빈 리스트, 중복 제거, S3 업로드 완료 여부 확인
    public List<String> normalize(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<String> distinct = keys.stream().map(String::strip).filter(k -> !k.isEmpty()).distinct().toList();
        if (distinct.size() > MAX_FILES) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "첨부파일은 최대 " + MAX_FILES + "개까지 가능합니다.");
        }
        for (String key : distinct) {
            if (fileStorage.findObjectSize(key).isEmpty()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "업로드되지 않은 파일입니다: " + key);
            }
        }
        return distinct;
    }
}
