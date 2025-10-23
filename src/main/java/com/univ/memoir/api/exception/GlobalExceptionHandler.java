package com.univ.memoir.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.responses.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 커스텀 예외 처리 (GlobalException)
     * - InvalidTokenException, UserNotFoundException 등
     */
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(GlobalException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        log.error("CustomException.{}: {}",
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 리소스 찾을 수 없음 (404)
     * - JPA의 EntityNotFoundException
     * - findById().orElseThrow()에서 발생
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("EntityNotFound: {}", ex.getMessage());

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND));
    }

    /**
     * 잘못된 입력값 (400)
     * - IllegalArgumentException 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    /**
     * 예상치 못한 서버 에러 (500)
     * - 모든 미처리 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("UnexpectedException.{}: {}",
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}