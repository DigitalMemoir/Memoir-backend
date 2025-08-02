package com.univ.memoir.api.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.res.DailyPopupResponse;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.DailySummaryService;
import com.univ.memoir.core.service.MonthlySummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "일일 요약", description = "일일 요약 관련 API")
public class DailySummaryController {

	private final DailySummaryService dailySummaryService;
	private final MonthlySummaryService monthlySummaryService;

	@PostMapping(value = "/daily", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "일일 요약", description = "일일 요약 페이지를 생성합니다.")
	public ResponseEntity<SuccessResponse<DailySummaryService.DailySummaryResult>> getDailySummary(
			@RequestHeader("Authorization") String accessToken,
			@RequestBody @Valid TimeAnalysisRequest request) {

		DailySummaryService.DailySummaryResult result = dailySummaryService.summarizeDay(accessToken, request);

		return ResponseEntity.ok(
				SuccessResponse.of(SuccessCode.TIME_ANALYSIS_SUCCESS, result).getBody()
		);
	}

	@GetMapping("/daily/popup/{date}")
	@Operation(summary = "일별 요약 페이지", description = "일별 요약 팝업을 조회합니다.")
	public ResponseEntity<SuccessResponse<DailyPopupResponse.Data>> getDailyPopup(
			@RequestHeader("Authorization") String accessToken,
			@PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
	) {
		DailyPopupResponse.Data data = monthlySummaryService.getDailyPopup(accessToken, date);
		return SuccessResponse.of(SuccessCode.DAILY_POPUP_OK, data);
	}

	@GetMapping("/daily/{date}")
	@Operation(summary = "일별 요약 페이지", description = "일별 요약 팝업을 조회합니다.")
	public ResponseEntity<SuccessResponse<DailySummaryService.DailySummaryResult>> getDaily(
			@RequestHeader("Authorization") String accessToken,
			@PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
	) {
		DailySummaryService.DailySummaryResult data = dailySummaryService.getDaily(accessToken, date);
		return SuccessResponse.of(SuccessCode.DAILY_POPUP_OK, data);
	}
}