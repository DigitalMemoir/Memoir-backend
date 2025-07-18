package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.res.time.ActivityStats;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.TimeService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TimeController {

    private final TimeService timeService;

    @PostMapping("/time")
    @Operation(summary = "웹 활동 통계 분석", description = "웹 활동 시간을 분석하여 통계를 반환합니다.")
    public ResponseEntity<SuccessResponse<ActivityStats>> analyzeTimeStats(
                                                                            @RequestHeader("Authorization") String accessToken,
                                                                            @RequestBody TimeAnalysisRequest request
    ) {
        ActivityStats result = timeService.analyzeTimeStats(accessToken, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result).getBody());
    }
}