package pv.domain.files.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileStatus {
    PENDING("업로드 URL만 발급된 상태"),
    UPLOADED("스토리지에 객체가 존재함을 서버가 확인한 상태");

    private final String description;
}
