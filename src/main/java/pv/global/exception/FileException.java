package pv.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FileException extends ResponseStatusException {
    private FileException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public static FileException notFound() {
        return new FileException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
    }

    public static FileException forbidden() {
        return new FileException(HttpStatus.FORBIDDEN, "본인이 발급받은 업로드만 등록할 수 있습니다.");
    }

    public static FileException badRequest(String reason) {
        return new FileException(HttpStatus.BAD_REQUEST, reason);
    }
}
