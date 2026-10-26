
package pv.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pv.domain.files.service.FileService;
import pv.global.exception.CustomException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AttachmentValidator {

    private static final int MAX_FILES = 10;

    private final FileService fileService;

    public List<String> normalize(List<String> keys, Long memberId) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<String> distinct = keys.stream()
                .map(String::strip)
                .filter(key -> !key.isEmpty())
                .distinct()
                .toList();

        if (distinct.size() > MAX_FILES) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "첨부파일은 최대 " + MAX_FILES + "개까지 가능합니다."
            );
        }

        fileService.attach(memberId, distinct);

        return distinct;
    }
}
