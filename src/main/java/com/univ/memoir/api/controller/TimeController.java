package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.res.time.ActivityStats; // 이 DTO가 DailySummaryService.DailySummaryResult.ActivityStats와 일치하는지 확인 필요
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.DailySummaryService; // TimeService 대신 DailySummaryService 사용
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; // HttpStatus import 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "웹 활동 기록", description = "웹 활동기록 관련 API")
public class TimeController {

    private final DailySummaryService dailySummaryService; // 변경된 서비스 주입

    @PostMapping("/time")
    @Operation(summary = "웹 활동 시간 분석", description = "웹 활동 시간을 분석합니다.")
    public ResponseEntity<SuccessResponse<DailySummaryService.DailySummaryResult>> analyzeTimeStats(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody TimeAnalysisRequest request
    ) {
        DailySummaryService.DailySummaryResult result = dailySummaryService.summarizeDay(accessToken, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result).getBody());
    }
}