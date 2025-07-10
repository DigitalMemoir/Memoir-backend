package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.VisitedPagesRequest;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.KeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keywords")
@Tag(name = "오늘의 키워드", description = "오늘의 키워드 관련 API")
public class KeywordController {

    private final KeywordService keywordService;

    @PostMapping("/analyze")
    @Operation(summary = "오늘의 키워드 분석", description = "오늘의 키워드를 분석합니다.")
    public Mono<ResponseEntity<SuccessResponse<Map>>> analyzeKeywords(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody VisitedPagesRequest request
    ) {
        return keywordService.analyzeKeywords(accessToken, request)
                .map(result -> SuccessResponse.of(SuccessCode.KEYWORD_EXTRACTION_SUCCESS, result));
    }

}

