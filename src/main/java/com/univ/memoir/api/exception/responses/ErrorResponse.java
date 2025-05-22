package com.univ.memoir.api.exception.responses;

import com.univ.memoir.api.exception.codes.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class ErrorResponse {
    private final int code;
    private final String msg;

    public static ErrorResponse create() {
        return new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getStatus().value())
                .msg(errorCode.getMessage())
                .build();
    }

    public static ResponseEntity<ErrorResponse> to(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus().value())
                .body(ErrorResponse.of(errorCode));
    }
}