package com.univ.memoir.api.exception.codes;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    /**
     *  400 Bad Request
     */
    // 잘못된 값 입력
    INVALID_INPUT(400, HttpStatus.BAD_REQUEST, "잘못된 값을 입력하였습니다."),

    // 인증 관련 오류
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "접근할 수 있는 권한이 없습니다. 유효한 access token을 확인하세요."),
    INVALID_JWT_ACCESS_TOKEN(401, HttpStatus.UNAUTHORIZED, "유효하지 않은 ACCESS TOKEN입니다."),
    EXPIRED_JWT_ACCESS_TOKEN(401, HttpStatus.UNAUTHORIZED, "ACCESS TOKEN이 만료되었습니다. 재발급 받아주세요."),
    INVALID_JWT_REFRESH_TOKEN(401, HttpStatus.UNAUTHORIZED, "유효하지 않은 REFRESH TOKEN입니다."),
    EXPIRED_JWT_REFRESH_TOKEN(401, HttpStatus.UNAUTHORIZED, "REFRESH TOKEN이 만료되었습니다. 다시 로그인해주세요."),

    /**
     *  403 Forbidden
     */
    WRONG_USER_PASSWORD(403, HttpStatus.FORBIDDEN, "입력하신 비밀번호가 올바르지 않습니다."),

    /**
     *  404 Not Found
     */
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "요청한 유저를 찾을 수 없습니다. 회원가입을 확인하세요."),
    USER_NOT_FOUND_BY_ID(404, HttpStatus.NOT_FOUND, "해당 ID를 가진 유저를 찾을 수 없습니다."),
    NOT_SIGN_IN_GOOGLE_ID(404, HttpStatus.NOT_FOUND, "회원가입되지 않은 구글 계정입니다. 회원가입을 진행해 주세요."),

    // 관련된 데이터가 없는 경우


    /**
     * 409 Conflict
     */
    DUPLICATE_GOOGLE_ID(409, HttpStatus.CONFLICT, "이미 회원가입 된 구글 계정입니다."),

    /**
     *  500 INTERNAL SERVER ERROR
     */
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다. 잠시 후 다시 시도해주세요."),
    NOTIFICATION_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송에 실패했습니다. 잠시 후 다시 시도해주세요."),
    DATABASE_CONNECTION_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 연결에 실패했습니다. 잠시 후 다시 시도해주세요."),
    UNEXPECTED_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 서버 오류가 발생했습니다. 관리자에게 문의하세요."),

    /**
     *  200 OK (알림 및 상태 관련)
     */
    NOTIFICATION_SENT(200, HttpStatus.OK, "알림이 성공적으로 전송되었습니다.");

    private final int statusCode;
    private final HttpStatus status;
    private final String message;
}