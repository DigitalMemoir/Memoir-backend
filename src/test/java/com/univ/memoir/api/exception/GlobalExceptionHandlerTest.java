package com.univ.memoir.api.exception;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.responses.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("should handle GlobalException with correct error code")
    void shouldHandleGlobalExceptionWithCorrectErrorCode() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("should handle GlobalException with INVALID_JWT_ACCESS_TOKEN")
    void shouldHandleGlobalExceptionWithInvalidJwtAccessToken() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.INVALID_JWT_ACCESS_TOKEN);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("유효하지 않은 ACCESS TOKEN");
    }

    @Test
    @DisplayName("should handle GlobalException with DUPLICATE_GOOGLE_ID")
    void shouldHandleGlobalExceptionWithDuplicateGoogleId() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.DUPLICATE_GOOGLE_ID);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("should handle EntityNotFoundException")
    void shouldHandleEntityNotFoundException() {
        // given
        EntityNotFoundException exception = new EntityNotFoundException("Entity not found");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleEntityNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("should handle IllegalArgumentException")
    void shouldHandleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("should handle generic Exception as internal server error")
    void shouldHandleGenericExceptionAsInternalServerError() {
        // given
        Exception exception = new Exception("Unexpected error");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @Test
    @DisplayName("should handle NullPointerException as internal server error")
    void shouldHandleNullPointerExceptionAsInternalServerError() {
        // given
        NullPointerException exception = new NullPointerException("Null pointer");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("should handle RuntimeException as internal server error")
    void shouldHandleRuntimeExceptionAsInternalServerError() {
        // given
        RuntimeException exception = new RuntimeException("Runtime error");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("should return 401 for expired access token")
    void shouldReturn401ForExpiredAccessToken() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.EXPIRED_JWT_ACCESS_TOKEN);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).contains("만료");
    }

    @Test
    @DisplayName("should return 401 for expired refresh token")
    void shouldReturn401ForExpiredRefreshToken() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.EXPIRED_JWT_REFRESH_TOKEN);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).contains("만료");
    }

    @Test
    @DisplayName("should return 403 for wrong user password")
    void shouldReturn403ForWrongUserPassword() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.WRONG_USER_PASSWORD);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("비밀번호");
    }

    @Test
    @DisplayName("should handle database connection error")
    void shouldHandleDatabaseConnectionError() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.DATABASE_CONNECTION_ERROR);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).contains("데이터베이스");
    }

    @Test
    @DisplayName("should handle unexpected error")
    void shouldHandleUnexpectedError() {
        // given
        GlobalException exception = new GlobalException(ErrorCode.UNEXPECTED_ERROR);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).contains("예상치 못한");
    }
}