package com.univ.memoir.api.exception.responses;

import org.springframework.http.ResponseEntity;

import com.univ.memoir.api.exception.codes.SuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class SuccessResponse<T> {
    private final int code;
    private final String msg;
    private T data;

    public static ResponseEntity<SuccessResponse> of(SuccessCode success) {
        return ResponseEntity.status(success.getStatus())
                .body(new SuccessResponse(success.getStatusCode(), success.getMessage()));
    }

    public static <T> ResponseEntity<SuccessResponse<T>> of(SuccessCode success, T data) {

        return ResponseEntity.status(success.getStatus())
                .body(new SuccessResponse<T>(success.getStatusCode(), success.getMessage(), data)); //<T>
    }

    public static <T> ResponseEntity<SuccessResponse<T>> of(T data) {
        return ResponseEntity.status(SuccessCode.OK.getStatus())
                .body(new SuccessResponse<>(SuccessCode.OK.getStatusCode(), SuccessCode.OK.getMessage(), data));
    }
}