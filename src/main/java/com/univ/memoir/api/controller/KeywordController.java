package com.univ.memoir.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.memoir.api.dto.req.page.VisitedPagesRequest;
import com.univ.memoir.api.dto.res.KeywordFrequencyDto;
import com.univ.memoir.api.dto.res.keyword.KeywordResponseDto;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.KeywordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keywords")
@Tag(name = "오늘의 키워드", description = "오늘의 키워드 관련 API")
public class KeywordController {

    private final KeywordService keywordService;

    @PostMapping("/analyze")
    @Operation(summary = "오늘의 키워드 분석", description = "오늘의 키워드를 분석합니다.")
    public ResponseEntity<SuccessResponse<KeywordResponseDto>> analyzeKeywords(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody VisitedPagesRequest request
    ) {
        KeywordResponseDto result = keywordService.analyzeKeywords(accessToken, request);
        return SuccessResponse.of(SuccessCode.KEYWORD_EXTRACTION_SUCCESS, result);
    }

    @GetMapping("/today/top9")
    @Operation(summary = "오늘의 키워드 상위 9개 조회", description = "현재 사용자의 오늘 날짜에 해당하는 상위 9개 키워드를 빈도수 순으로 조회합니다.")
    public ResponseEntity<SuccessResponse<List<KeywordFrequencyDto>>> getTop9KeywordsForToday(
            @RequestHeader("Authorization") String accessToken
    ) {
        // KeywordService의 getTopKeywordsForToday 메서드를 호출하여 상위 9개 키워드 목록을 가져옵니다.
        List<KeywordFrequencyDto> topKeywords = keywordService.getTopKeywordsForToday(accessToken);

        return SuccessResponse.of(SuccessCode.TOP_KEYWORDS_RETRIEVED_SUCCESS, topKeywords);
    }
}
