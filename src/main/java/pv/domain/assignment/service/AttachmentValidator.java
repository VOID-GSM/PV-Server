
package pv.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pv.domain.files.service.FileService;
import pv.global.exception.CustomException;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AttachmentValidator {

    private static final int MAX_FILES = 10;

    private final FileService fileService;

    public List<String> attachNew(Long memberId, List<String> keys) {
        List<String> next = normalize(keys);
        fileService.attach(memberId, next);
        return next;
    }

    public List<String> replace(Long memberId, List<String> current, List<String> requested) {
        List<String> next = normalize(requested);
        Set<String> currentSet = current == null ? Set.of() : Set.copyOf(current);
        Set<String> nextSet = Set.copyOf(next);

        List<String> added = next.stream().filter(key -> !currentSet.contains(key)).toList();
        List<String> removed = currentSet.stream().filter(key -> !nextSet.contains(key)).toList();

        fileService.attach(memberId, added);
        fileService.deleteByKeys(memberId, removed);
        return next;
    }

    private List<String> normalize(List<String> keys) {
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
        return distinct;
    }
}
