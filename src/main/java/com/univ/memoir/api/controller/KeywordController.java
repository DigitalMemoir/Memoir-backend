package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.VisitedPagesRequest;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.KeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keywords")
public class KeywordController {

    private final KeywordService keywordService;

    @PostMapping("/analyze")
    public
    Mono<ResponseEntity<SuccessResponse<Map>>> analyzeKeywords(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody VisitedPagesRequest request
    ) {
        return keywordService.analyzeKeywords(accessToken, request)
                .map(result -> SuccessResponse.of(SuccessCode.KEYWORD_EXTRACTION_SUCCESS, result));
    }

}

