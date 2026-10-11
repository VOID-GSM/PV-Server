package pv.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        ErrorResponse response = new ErrorResponse(
                e.getStatus().value(),
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(e.getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    public record ErrorResponse(
            int status,
            String message,
            LocalDateTime timestamp
    ) {
    }
}