package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.DailySummaryService;
import com.univ.memoir.core.service.DailySummaryService.DailySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DailySummaryController {

	private final DailySummaryService dailySummaryService;

	/**
	 * POST /api/daily/summary
	 * {
	 * "date": "2024-06-12",
	 * "visitedPages": [ ... ]
	 * }
	 *
	 * @param accessToken (예: Authorization 헤더에서 추출해도 됨)
	 */
	@PostMapping(value = "/daily", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<SuccessResponse<DailySummaryResponse>>> getDailySummary(
		@RequestHeader(value = "Authorization", required = false) String accessToken,
		@RequestBody TimeAnalysisRequest request) {

		return dailySummaryService.summarizeDay(accessToken, request)
			.map(result -> SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result));
	}
}
