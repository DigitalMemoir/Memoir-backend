package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.res.time.ActivityStats;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.TimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "웹 활동 기록", description = "웹 활동기록 관련 API")
public class TimeController {

    private final TimeService timeService;

    @PostMapping("/time")
    @Operation(summary = "웹 활동 시간 분석", description = "웹 활동 시간을 분석합니다.")
    public Mono<ResponseEntity<SuccessResponse<ActivityStats>>> analyzeTimeStats(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody TimeAnalysisRequest request
    ) {
        return timeService.analyzeTimeStats(accessToken, request)
                .map(result -> SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result));
    }
}