package com.univ.memoir.core.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.req.time.VisitedPageForTimeDto;
import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.customException.UserNotFoundException;
import com.univ.memoir.core.domain.DailySummary;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.DailySummaryRepository;


@Service
public class DailySummaryService {

	private static final Logger log = LoggerFactory.getLogger(DailySummaryService.class);

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final DailySummaryRepository dailySummaryRepository;
	private final UserService userService;

	public DailySummaryService(
			@Qualifier("openAiRestTemplate") RestTemplate restTemplate,
			ObjectMapper objectMapper,
			DailySummaryRepository dailySummaryRepository, UserService userService
	) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.dailySummaryRepository = dailySummaryRepository;
		this.userService = userService;
	}

	@Value("${openai.model}")
	private String OPENAI_MODEL;

	@Value("${openai.api.base-url}")
	private String OPENAI_BASE_URL;

	@Value("${openai.uri}")
	private String OPENAI_URI;

	// --- ★ 카테고리 검증용 상수 추가 ★ ---
	private static final List<String> VALID_CATEGORIES = List.of(
			"공부, 학습", "뉴스, 정보 탐색", "콘텐츠 소비", "쇼핑", "업무, 프로젝트"
	);

	private static final String DEFAULT_CATEGORY = "콘텐츠 소비";

	/**
	 * 사용자의 일일 활동을 요약합니다.
	 *
	 * @param accessToken 사용자 인증 토큰
	 * @param request 시간 분석 요청 DTO
	 * @return 요약된 일일 활동 결과
	 */
	public DailySummaryResult summarizeDay(String accessToken, TimeAnalysisRequest request) {
		User currentUser = userService.findByAccessToken(accessToken);

		if (currentUser == null) {
			throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);
		}

		List<VisitedPageForTimeDto> pages = request.getVisitedPages();
		if (pages == null || pages.isEmpty()) {
			throw new IllegalArgumentException("방문 기록이 없습니다.");
		}

		LocalDate localDate = LocalDate.parse(request.getDate());

		// 1. GPT를 통해 페이지 카테고리 분류 (동기 호출)
		List<CategorizedPage> categorizedPages = fetchCategoriesFromGPT(pages);

		// 2. 활동 통계 계산
		DailyActivityStats stats = calculateStats(categorizedPages);

		// 3. GPT를 통해 일일 요약 생성 (동기 호출)
		GptSummary gptSummary = fetchDailySummaryFromGPT(request.getDate(), categorizedPages);

		// 4. 결과 객체 생성
		DailySummaryResult result = new DailySummaryResult(
				request.getDate(),
				gptSummary.topKeywords,
				gptSummary.dailyTimeline,
				gptSummary.summaryText,
				new DailySummaryResult.ActivityStats(
						stats.totalUsageMinutes,
						stats.getCategoryPercentages()
				)
		);

		// 5. DB 저장 (User 정보 포함)
		try {
			dailySummaryRepository.save(new DailySummary(
					currentUser,  // ← User 추가
					localDate,
					objectMapper.writeValueAsString(result.topKeywords()),
					objectMapper.writeValueAsString(result.dailyTimeline()),
					objectMapper.writeValueAsString(result.summaryText()),
					result.activityStats().totalUsageTimeMinutes(),
					objectMapper.writeValueAsString(result.activityStats().activityProportions())
			));
		} catch (JsonProcessingException e) {
			log.error("DB 저장용 JSON 직렬화 실패", e);
			throw new RuntimeException("DB 저장용 JSON 직렬화 실패", e);
		}

		return result;
	}

	/**
	 * 특정 날짜의 일일 요약을 조회합니다.
	 *
	 * @param accessToken 사용자 인증 토큰
	 * @param date 조회할 날짜
	 * @return 일일 요약 결과
	 */
	public DailySummaryResult getDaily(String accessToken, LocalDate date) {
		User user = userService.findByAccessToken(accessToken);

		if (user == null) {
			throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);
		}

		Optional<DailySummary> optionalData = dailySummaryRepository.findByUserAndDate(user, date);

		if (optionalData.isEmpty()) {
			// 데이터 없을 경우, 빈 객체 반환
			return new DailySummaryResult(
					date.toString(),
					Collections.emptyList(),
					Collections.emptyList(),
					Collections.emptyList(),
					new DailySummaryResult.ActivityStats(0, Collections.emptyList())
			);
		}

		DailySummary data = optionalData.get();

		try {
			List<DailySummaryResult.TopKeyword> topKeywords = objectMapper.readValue(
					data.getTopKeywordsJson(),
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, DailySummaryResult.TopKeyword.class
					)
			);

			List<DailySummaryResult.DailyTimelineEntry> dailyTimeline = objectMapper.readValue(
					data.getTimelineJson(),
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, DailySummaryResult.DailyTimelineEntry.class
					)
			);

			List<String> summaryText = objectMapper.readValue(
					data.getSummaryTextJson(),
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, String.class
					)
			);

			List<DailySummaryResult.ActivityProportion> activityProportions = objectMapper.readValue(
					data.getActivityProportionsJson(),
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, DailySummaryResult.ActivityProportion.class
					)
			);

			return new DailySummaryResult(
					date.toString(),
					topKeywords,
					dailyTimeline,
					summaryText,
					new DailySummaryResult.ActivityStats(
							data.getTotalUsageMinutes(),
							activityProportions
					)
			);
		} catch (JsonProcessingException e) {
			log.error("일일 요약 데이터 역직렬화 실패", e);
			throw new RuntimeException("일일 요약 데이터 역직렬화 실패", e);
		}
	}

	/**
	 * GPT를 통해 방문 페이지의 카테고리를 분류합니다.
	 */
	private List<CategorizedPage> fetchCategoriesFromGPT(List<VisitedPageForTimeDto> pages) {
		String prompt;
		try {
			String pagesJson = objectMapper.writeValueAsString(pages);
			prompt = """
                   아래는 사용자의 방문 기록입니다. 각 페이지의 제목과 URL을 참고하여 해당 페이지의 카테고리를 분류하세요.
                   카테고리는 다음 중 하나로만 정하세요:
                   '공부, 학습', '뉴스, 정보 탐색', '콘텐츠 소비', '쇼핑', '업무, 프로젝트'

                   다음 형식으로만 응답하세요 (JSON strict array):
                   [
                     { "title": "...", "url": "...", "category": "..." },
                     ...
                   ]

                   방문 기록:
                   %s
                   """.formatted(pagesJson);
		} catch (JsonProcessingException e) {
			log.error("페이지 목록 JSON 직렬화 실패", e);
			throw new RuntimeException("페이지 목록 JSON 직렬화 실패", e);
		}

		Map<String, Object> requestBody = Map.of(
				"model", OPENAI_MODEL,
				"messages", List.of(
						Map.of("role", "system", "content", "당신은 인터넷 기록 분류 전문가입니다."),
						Map.of("role", "user", "content", prompt)
				),
				"temperature", 0.2
		);

		try {
			Map<String, Object> response = restTemplate.postForObject(
					OPENAI_BASE_URL + OPENAI_URI,
					requestBody,
					Map.class
			);

			List<?> choices = (List<?>) response.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw new RuntimeException("GPT 응답에 'choices' 필드가 없습니다.");
			}

			Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
			String content = Objects.toString(message.get("content"), "").trim();

			List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

			// GPT 응답 개수 조정
			if (parsedList.size() < pages.size()) {
				int diff = pages.size() - parsedList.size();
				for (int i = 0; i < diff; i++) {
					parsedList.add(Map.of("title", "", "url", "", "category", DEFAULT_CATEGORY));
				}
			} else if (parsedList.size() > pages.size()) {
				parsedList = parsedList.subList(0, pages.size());
			}

			List<CategorizedPage> result = new ArrayList<>();
			for (int i = 0; i < pages.size(); i++) {
				String category = parsedList.get(i).getOrDefault("category", DEFAULT_CATEGORY);

				// ★ 유효하지 않은 카테고리 기본값으로 대체 ★
				if (!VALID_CATEGORIES.contains(category)) {
					log.warn("잘못된 카테고리 '{}' → 기본값 '{}'으로 대체", category, DEFAULT_CATEGORY);
					category = DEFAULT_CATEGORY;
				}

				result.add(new CategorizedPage(pages.get(i), category));
			}
			return result;
		} catch (Exception e) {
			log.error("GPT 카테고리 분류 응답 파싱 실패", e);
			throw new RuntimeException("GPT 카테고리 분류 응답 파싱 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * 방문 페이지 데이터로부터 일일 활동 통계를 계산합니다.
	 */
	private DailyActivityStats calculateStats(List<CategorizedPage> pages) {
		int totalSeconds = 0;
		Map<String, Integer> categoryToSeconds = new HashMap<>();

		for (CategorizedPage page : pages) {
			int duration = page.page.getDurationSeconds();
			totalSeconds += duration;
			categoryToSeconds.merge(page.category, duration, Integer::sum);
		}

		return new DailyActivityStats(totalSeconds / 60, categoryToSeconds);
	}

	/**
	 * GPT를 통해 일일 활동 요약을 생성합니다.
	 */
	private GptSummary fetchDailySummaryFromGPT(String date, List<CategorizedPage> pages) {
		StringBuilder visitSummary = new StringBuilder();
		for (CategorizedPage cp : pages) {
			visitSummary.append(String.format("- 제목: %s, 카테고리: %s%n", cp.page.getTitle(), cp.category));
		}

		String prompt = """
           당신은 디지털 활동 요약 전문가입니다.
           사용자가 %s 하루 동안 다음과 같은 인터넷 방문 기록과 카테고리 정보를 보냈습니다:

           %s

           위 데이터를 참고해 다음을 작성해주세요.
           1) 오늘의 키워드 상위 3개 (내림차순, { "keyword": "...", "frequency": 숫자 } JSON 배열 형식)
           2) 시간대별 활동 타임라인 (ex: "09:00 - 뉴스 읽기")
           3) 3줄짜리 전체 활동 요약 문장 (한국어)

           JSON 형식으로 아래 필드를 포함하여 응답하세요:
           {
             "topKeywords": [ { "keyword": "...", "frequency": 숫자 }, ... ],
             "dailyTimeline": [ { "time": "HH:mm", "description": "..." }, ... ],
             "summaryText": [ "문장1", "문장2", "문장3" ]
           }
           """.formatted(date, visitSummary);

		Map<String, Object> requestBody = Map.of(
				"model", OPENAI_MODEL,
				"messages", List.of(
						Map.of("role", "system", "content", "당신은 친절한 일일 활동 요약 전문가입니다."),
						Map.of("role", "user", "content", prompt)
				),
				"temperature", 0.3
		);

		try {
			Map<String, Object> response = restTemplate.postForObject(
					OPENAI_BASE_URL + OPENAI_URI,
					requestBody,
					Map.class
			);

			List<?> choices = (List<?>) response.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw new RuntimeException("GPT 응답에 'choices' 필드가 없습니다.");
			}

			Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
			String content = Objects.toString(message.get("content"), "").trim();

			log.debug("GPT raw content: {}", content);

			// ```json ... ``` 제거
			if (content.startsWith("```")) {
				int start = content.indexOf("\n") + 1;
				int end = content.lastIndexOf("```");
				if (start > 0 && end > start) {
					content = content.substring(start, end).trim();
				}
			}

			// 설명문 제거: { 로 시작하지 않으면 첫 {부터 자르기
			if (!content.startsWith("{")) {
				int idx = content.indexOf("{");
				if (idx != -1) {
					content = content.substring(idx).trim();
				}
			}

			Map<String, Object> parsed = objectMapper.readValue(content, Map.class);

			List<DailySummaryResult.TopKeyword> keywords = ((List<Map<String, Object>>) parsed.getOrDefault("topKeywords", Collections.emptyList()))
					.stream()
					.map(k -> new DailySummaryResult.TopKeyword(
							Objects.toString(k.get("keyword"), ""),
							((Number) k.getOrDefault("frequency", 0)).intValue()))
					.collect(Collectors.toList());

			List<DailySummaryResult.DailyTimelineEntry> timeline = ((List<Map<String, Object>>) parsed.getOrDefault("dailyTimeline", Collections.emptyList()))
					.stream()
					.map(t -> new DailySummaryResult.DailyTimelineEntry(
							Objects.toString(t.get("time"), ""),
							Objects.toString(t.get("description"), "")))
					.collect(Collectors.toList());

			List<String> summaryText = (List<String>) parsed.getOrDefault("summaryText", Collections.emptyList());

			return new GptSummary(keywords, timeline, summaryText);
		} catch (Exception e) {
			log.error("GPT 일일 요약 응답 파싱 실패. 원본 content: {}", e.getMessage(), e);
			throw new RuntimeException("GPT 일일 요약 응답 파싱 실패: " + e.getMessage(), e);
		}
	}

	// 내부 클래스들
	private static class CategorizedPage {
		VisitedPageForTimeDto page;
		String category;

		public CategorizedPage(VisitedPageForTimeDto page, String category) {
			this.page = page;
			this.category = category;
		}
	}

	private static class DailyActivityStats {
		int totalUsageMinutes;
		Map<String, Integer> categorySeconds;

		public DailyActivityStats(int totalUsageMinutes, Map<String, Integer> categorySeconds) {
			this.totalUsageMinutes = totalUsageMinutes;
			this.categorySeconds = categorySeconds;
		}

		public List<DailySummaryResult.ActivityProportion> getCategoryPercentages() {
			List<DailySummaryResult.ActivityProportion> list = new ArrayList<>();
			if (totalUsageMinutes == 0) return list;

			for (Map.Entry<String, Integer> e : categorySeconds.entrySet()) {
				int percent = (int) Math.round((e.getValue() / 60.0) * 100 / totalUsageMinutes);
				list.add(new DailySummaryResult.ActivityProportion(e.getKey(), percent));
			}
			return list;
		}
	}

	private static class GptSummary {
		List<DailySummaryResult.TopKeyword> topKeywords;
		List<DailySummaryResult.DailyTimelineEntry> dailyTimeline;
		List<String> summaryText;

		public GptSummary(List<DailySummaryResult.TopKeyword> topKeywords,
						  List<DailySummaryResult.DailyTimelineEntry> dailyTimeline,
						  List<String> summaryText) {
			this.topKeywords = topKeywords;
			this.dailyTimeline = dailyTimeline;
			this.summaryText = summaryText;
		}
	}

	public static record DailySummaryResult(
			String date,
			List<TopKeyword> topKeywords,
			List<DailyTimelineEntry> dailyTimeline,
			List<String> summaryText,
			ActivityStats activityStats
	) {
		public record TopKeyword(String keyword, int frequency) {}
		public record DailyTimelineEntry(String time, String description) {}
		public record ActivityStats(int totalUsageTimeMinutes, List<ActivityProportion> activityProportions) {}
		public record ActivityProportion(String category, int percentage) {}
	}
}