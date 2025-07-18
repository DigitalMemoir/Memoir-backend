package com.univ.memoir.api.exception.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum SuccessCode {

    /**
     * 200 OK
     */
    OK(HttpStatus.OK, "요청이 성공했습니다."),
    BOOKMARK_UPDATE_SUCCESS(HttpStatus.OK,"북마크가 수정되었습니다."),
    BOOKMARK_RETRIEVE_SUCCESS(HttpStatus.OK,"북마크 조회에 성공했습니다."),
    TOP_KEYWORDS_RETRIEVED_SUCCESS(HttpStatus.OK,"오늘의 키워드 조회에 성공했습니다."),

    /**
     * 201 CREATED SUCCESS
     */
    CREATED(HttpStatus.CREATED, "생성 요청이 성공했습니다."),
    USER_CREATED(HttpStatus.CREATED, "회원 등록에 성공했습니다."),
    UPDATED(HttpStatus.CREATED, "업데이트 요청이 성공했습니다."),
    KEYWORD_EXTRACTION_SUCCESS(HttpStatus.CREATED, "키워드 추출에 성공했습니다."),
    TIME_ANALYSIS_SUCCESS(HttpStatus.CREATED, "사용 시간 분석에 성공했습니다."),
    BOOKMARK_ADD_SUCCESS(HttpStatus.CREATED,"북마크가 추가되었습니다."),

    /**
     * 202 ACCEPTED
     */
    ACCEPTED(HttpStatus.ACCEPTED, "요청이 성공적으로 처리되었습니다. 결과는 나중에 확인할 수 있습니다."),

    /**
     * 204 NO CONTENT
     */
    NO_CONTENT(HttpStatus.NO_CONTENT, "요청이 성공적으로 처리되었으나 반환할 데이터가 없습니다."),

    /**
     * 200 OK (Additional Success Responses)
     */
    NOTIFICATION_SENT(HttpStatus.OK, "알림이 성공적으로 전송되었습니다."),
    MONTHLY_SUMMARY_OK(HttpStatus.OK, "월별 활동 요약 조회 성공"),
    DAILY_POPUP_OK(HttpStatus.OK, "일별 활동 요약 조회 성공"),

    /**
     * 204 NO CONTENT (Deletion Responses)
     */
    USER_DELETED(HttpStatus.NO_CONTENT, "유저가 성공적으로 삭제되었습니다."),
    BOOKMARK_REMOVE_SUCCESS(HttpStatus.NO_CONTENT,"북마크가 삭제되었습니다.");

    private final HttpStatus status;
    private final String message;

    public int getStatusCode() {
        return status.value();
    }

}
