package com.univ.memoir.api.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.res.DailyPopupResponse;
import com.univ.memoir.api.dto.res.MonthlySummaryResponse;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.DailySummaryService;
import com.univ.memoir.core.service.MonthlySummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "일일 요약", description = "일일 요약 관련 API")
public class DailySummaryController {

	private final DailySummaryService dailySummaryService;
	private final MonthlySummaryService monthlySummaryService;

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
	@Operation(summary = "일일 요약", description = "일일 요약 페이지를 생성합니다.")
	public ResponseEntity<SuccessResponse<DailySummaryService.DailySummaryResult>> getDailySummary(
			@RequestHeader(value = "Authorization", required = false) String accessToken,
			@RequestBody TimeAnalysisRequest request) {

		DailySummaryService.DailySummaryResult result = dailySummaryService.summarizeDay(accessToken, request);

		return ResponseEntity.ok(
				SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result).getBody()
		);
	}


	@GetMapping("/daily/{date}")
	@Operation(summary = "일별 요약 페이지", description = "일별 요약 팝업을 조회합니다.")
	public ResponseEntity<SuccessResponse<DailyPopupResponse.Data>> getDailyPopup(
		@PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
	) {
		DailyPopupResponse.Data data = monthlySummaryService.getDailyPopup(date);
		return SuccessResponse.of(SuccessCode.DAILY_POPUP_OK, data);
	}
}

