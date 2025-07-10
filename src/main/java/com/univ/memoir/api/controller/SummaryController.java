package com.univ.memoir.api.controller;

import java.time.YearMonth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.memoir.api.dto.res.MonthlySummaryResponse;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.MonthlySummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "캘린더 요약", description = "캘린더 요약 API")
public class SummaryController {

	private final MonthlySummaryService monthlySummaryService;

	@GetMapping("/monthly/{date}")
	@Operation(summary = "월별 요약 페이지", description = "월별 요약 페이지를 조회합니다.")
	public ResponseEntity<SuccessResponse<MonthlySummaryResponse.Data>> getMonthlySummary(
		@PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
	) {
		MonthlySummaryResponse.Data data = monthlySummaryService.getMonthlySummary(yearMonth);
		return SuccessResponse.of(SuccessCode.MONTHLY_SUMMARY_OK, data);
	}
}
