package com.univ.memoir.api.exception.codes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode Enum Unit Tests")
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("should have non-null message for all error codes")
    void shouldHaveNonNullMessageForAllErrorCodes(ErrorCode errorCode) {
        // then
        assertThat(errorCode.getMessage()).isNotNull();
        assertThat(errorCode.getMessage()).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("should have non-null HttpStatus for all error codes")
    void shouldHaveNonNullHttpStatusForAllErrorCodes(ErrorCode errorCode) {
        // then
        assertThat(errorCode.getStatus()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("should have status code matching HttpStatus for all error codes")
    void shouldHaveStatusCodeMatchingHttpStatusForAllErrorCodes(ErrorCode errorCode) {
        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(errorCode.getStatus().value());
    }

    @Test
    @DisplayName("should have INVALID_INPUT as 400 Bad Request")
    void shouldHaveInvalidInputAs400BadRequest() {
        // when
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(400);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode.getMessage()).contains("잘못된");
    }

    @Test
    @DisplayName("should have UNAUTHORIZED as 401")
    void shouldHaveUnauthorizedAs401() {
        // when
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(401);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("should have INVALID_JWT_ACCESS_TOKEN as 401 Unauthorized")
    void shouldHaveInvalidJwtAccessTokenAs401Unauthorized() {
        // when
        ErrorCode errorCode = ErrorCode.INVALID_JWT_ACCESS_TOKEN;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(401);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode.getMessage()).contains("ACCESS TOKEN");
    }

    @Test
    @DisplayName("should have EXPIRED_JWT_ACCESS_TOKEN as 401 Unauthorized")
    void shouldHaveExpiredJwtAccessTokenAs401Unauthorized() {
        // when
        ErrorCode errorCode = ErrorCode.EXPIRED_JWT_ACCESS_TOKEN;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(401);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode.getMessage()).contains("만료");
    }

    @Test
    @DisplayName("should have INVALID_JWT_REFRESH_TOKEN as 401 Unauthorized")
    void shouldHaveInvalidJwtRefreshTokenAs401Unauthorized() {
        // when
        ErrorCode errorCode = ErrorCode.INVALID_JWT_REFRESH_TOKEN;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(401);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode.getMessage()).contains("REFRESH TOKEN");
    }

    @Test
    @DisplayName("should have EXPIRED_JWT_REFRESH_TOKEN as 401 Unauthorized")
    void shouldHaveExpiredJwtRefreshTokenAs401Unauthorized() {
        // when
        ErrorCode errorCode = ErrorCode.EXPIRED_JWT_REFRESH_TOKEN;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(401);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode.getMessage()).contains("만료");
    }

    @Test
    @DisplayName("should have WRONG_USER_PASSWORD as 403 Forbidden")
    void shouldHaveWrongUserPasswordAs403Forbidden() {
        // when
        ErrorCode errorCode = ErrorCode.WRONG_USER_PASSWORD;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(403);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode.getMessage()).contains("비밀번호");
    }

    @Test
    @DisplayName("should have NOT_FOUND as 404")
    void shouldHaveNotFoundAs404() {
        // when
        ErrorCode errorCode = ErrorCode.NOT_FOUND;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(404);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.FOUND);
        assertThat(errorCode.getMessage()).contains("리소스");
    }

    @Test
    @DisplayName("should have USER_NOT_FOUND as 404 Not Found")
    void shouldHaveUserNotFoundAs404NotFound() {
        // when
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(404);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(errorCode.getMessage()).contains("유저");
    }

    @Test
    @DisplayName("should have USER_NOT_FOUND_BY_ID as 404 Not Found")
    void shouldHaveUserNotFoundByIdAs404NotFound() {
        // when
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND_BY_ID;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(404);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(errorCode.getMessage()).contains("ID");
    }

    @Test
    @DisplayName("should have NOT_SIGN_IN_GOOGLE_ID as 404 Not Found")
    void shouldHaveNotSignInGoogleIdAs404NotFound() {
        // when
        ErrorCode errorCode = ErrorCode.NOT_SIGN_IN_GOOGLE_ID;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(404);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(errorCode.getMessage()).contains("구글");
    }

    @Test
    @DisplayName("should have DUPLICATE_GOOGLE_ID as 409 Conflict")
    void shouldHaveDuplicateGoogleIdAs409Conflict() {
        // when
        ErrorCode errorCode = ErrorCode.DUPLICATE_GOOGLE_ID;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(409);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errorCode.getMessage()).contains("구글");
    }

    @Test
    @DisplayName("should have INTERNAL_SERVER_ERROR as 500")
    void shouldHaveInternalServerErrorAs500() {
        // when
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(500);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorCode.getMessage()).contains("서버");
    }

    @Test
    @DisplayName("should have NOTIFICATION_ERROR as 500 Internal Server Error")
    void shouldHaveNotificationErrorAs500InternalServerError() {
        // when
        ErrorCode errorCode = ErrorCode.NOTIFICATION_ERROR;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(500);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorCode.getMessage()).contains("알림");
    }

    @Test
    @DisplayName("should have DATABASE_CONNECTION_ERROR as 500 Internal Server Error")
    void shouldHaveDatabaseConnectionErrorAs500InternalServerError() {
        // when
        ErrorCode errorCode = ErrorCode.DATABASE_CONNECTION_ERROR;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(500);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorCode.getMessage()).contains("데이터베이스");
    }

    @Test
    @DisplayName("should have UNEXPECTED_ERROR as 500 Internal Server Error")
    void shouldHaveUnexpectedErrorAs500InternalServerError() {
        // when
        ErrorCode errorCode = ErrorCode.UNEXPECTED_ERROR;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(500);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorCode.getMessage()).contains("예상치 못한");
    }

    @Test
    @DisplayName("should have NOTIFICATION_SENT as 200 OK")
    void shouldHaveNotificationSentAs200Ok() {
        // when
        ErrorCode errorCode = ErrorCode.NOTIFICATION_SENT;

        // then
        assertThat(errorCode.getStatusCode()).isEqualTo(200);
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(errorCode.getMessage()).contains("알림");
    }

    @Test
    @DisplayName("should have exactly 16 error codes defined")
    void shouldHaveExactly16ErrorCodesDefined() {
        // when
        ErrorCode[] errorCodes = ErrorCode.values();

        // then
        assertThat(errorCodes).hasSize(16);
    }

    @Test
    @DisplayName("should be able to get error code by name")
    void shouldBeAbleToGetErrorCodeByName() {
        // when
        ErrorCode errorCode = ErrorCode.valueOf("USER_NOT_FOUND");

        // then
        assertThat(errorCode).isNotNull();
        assertThat(errorCode).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("should have all JWT-related errors as 401")
    void shouldHaveAllJwtRelatedErrorsAs401() {
        // when & then
        assertThat(ErrorCode.INVALID_JWT_ACCESS_TOKEN.getStatusCode()).isEqualTo(401);
        assertThat(ErrorCode.EXPIRED_JWT_ACCESS_TOKEN.getStatusCode()).isEqualTo(401);
        assertThat(ErrorCode.INVALID_JWT_REFRESH_TOKEN.getStatusCode()).isEqualTo(401);
        assertThat(ErrorCode.EXPIRED_JWT_REFRESH_TOKEN.getStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("should have all server errors as 500")
    void shouldHaveAllServerErrorsAs500() {
        // when & then
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode()).isEqualTo(500);
        assertThat(ErrorCode.NOTIFICATION_ERROR.getStatusCode()).isEqualTo(500);
        assertThat(ErrorCode.DATABASE_CONNECTION_ERROR.getStatusCode()).isEqualTo(500);
        assertThat(ErrorCode.UNEXPECTED_ERROR.getStatusCode()).isEqualTo(500);
    }
}