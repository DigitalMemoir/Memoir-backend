package com.univ.memoir.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.res.time.ActivityStats;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.TimeService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TimeController {

    private final TimeService timeService;

    @PostMapping("/time")
    @Operation(summary = "웹 활동 통계 분석", description = "웹 활동 시간을 분석하여 통계를 반환합니다.")
    public ResponseEntity<SuccessResponse<ActivityStats>> analyzeTimeStats(
            @AuthenticationPrincipal String email,
            @RequestBody TimeAnalysisRequest request
    ) {
        ActivityStats result = timeService.analyzeTimeStats(email, request);

        return SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result);
    }
}