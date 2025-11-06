package com.univ.memoir.core.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.memoir.api.dto.res.DailyPopupResponse;
import com.univ.memoir.api.dto.res.MonthlySummaryResponse;
import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.custom.UserNotFoundException;
import com.univ.memoir.core.domain.DailySummary;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.DailySummaryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

	private final DailySummaryRepository dailySummaryRepository;
	private final ObjectMapper objectMapper;
	private final UserService userService;

	/**
	 * 월별 요약 조회
	 *
	 * @param email 사용자 이메일 (SecurityContext에서 추출)
	 * @param yearMonth 조회할 년월
	 * @return 월별 요약 데이터
	 */
	public MonthlySummaryResponse.Data getMonthlySummary(String email, YearMonth yearMonth) {
		// ✅ 이메일로 User 조회 (interests/bookmarks 불필요)
		User user = userService.findByEmailForSummary(email);

		if (user == null) {
			throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);
		}

		LocalDate start = yearMonth.atDay(1);
		LocalDate end = yearMonth.atEndOfMonth();

		List<DailySummary> summaries = dailySummaryRepository.findAllByUserAndDateBetween(user, start, end);

		List<MonthlySummaryResponse.CalendarEntry> entries = summaries.stream()
				.collect(
						java.util.stream.Collectors.toMap(
								summary -> summary.getDate().toString(), // key: 날짜 문자열
								summary -> {
									String title = extractTopKeyword(summary.getTopKeywordsJson());
									return new MonthlySummaryResponse.CalendarEntry(summary.getDate().toString(), title);
								},
								(existing, replacement) -> existing // 중복일 경우 existing 값을 제공
						)
				)
				.values()
				.stream()
				.toList();

		return new MonthlySummaryResponse.Data(
				yearMonth.getYear(),
				yearMonth.getMonthValue(),
				entries
		);
	}

	/**
	 * 일별 팝업 조회
	 *
	 * @param email 사용자 이메일 (SecurityContext에서 추출)
	 * @param date 조회할 날짜
	 * @return 일별 팝업 데이터
	 */
	public DailyPopupResponse.Data getDailyPopup(String email, LocalDate date) {
		// ✅ 이메일로 User 조회 (interests/bookmarks 불필요)
		User user = userService.findByEmailForSummary(email);

		if (user == null) {
			throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);
		}

		DailySummary summary = dailySummaryRepository.findByUserAndDate(user, date)
				.orElseThrow(() -> new EntityNotFoundException("해당 날짜의 요약이 존재하지 않습니다."));

		List<String> summaryTexts = parseSummaryTextJson(summary.getSummaryTextJson());

		return new DailyPopupResponse.Data(date.toString(), summaryTexts);
	}

	private String extractTopKeyword(String topKeywordsJson) {
		try {
			List<Map<String, Object>> list = objectMapper.readValue(topKeywordsJson, List.class);
			if (!list.isEmpty()) {
				return Objects.toString(list.get(0).get("keyword"), "기록 없음");
			}
		} catch (JsonProcessingException e) {
			return "기록 없음";
		}
		return "기록 없음";
	}

	private List<String> parseSummaryTextJson(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<List<String>>() {});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("summaryTextJson 파싱 실패", e);
		}
	}
}