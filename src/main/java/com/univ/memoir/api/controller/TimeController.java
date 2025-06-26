package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TimeController {

    private final TimeService timeService;

    @PostMapping("/time")
    public Mono<ResponseEntity<SuccessResponse<Map>>> analyzeTimeStats(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody TimeAnalysisRequest request
    ) {
        return timeService.analyzeTimeStats(accessToken, request)
                .map(result -> SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result));
    }
}