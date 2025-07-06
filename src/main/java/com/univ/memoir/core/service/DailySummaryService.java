package com.univ.memoir.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.req.time.VisitedPageForTimeDto;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailySummaryService {
	private static final Logger log = LoggerFactory.getLogger(DailySummaryService.class);

	private final WebClient openAiWebClient;
	private final ObjectMapper objectMapper;

	private static final String OPENAI_MODEL = "gpt-3.5-turbo";
	private static final String OPENAI_URI = "/chat/completions";

	public Mono<DailySummaryResponse> summarizeDay(String accessToken, TimeAnalysisRequest request) {
		List<VisitedPageForTimeDto> pages = request.getVisitedPages();
		if (pages == null || pages.isEmpty()) {
			return Mono.error(new IllegalArgumentException("방문 기록이 없습니다."));
		}

		return fetchCategoriesFromGPT(pages)
			.flatMap(categorizedPages -> {
				DailyActivityStats stats = calculateStats(categorizedPages);
				return fetchDailySummaryFromGPT(request.getDate(), categorizedPages)
					.map(gptSummary -> new DailySummaryResponse(
						200,
						"일일 활동 요약 조회 성공",
						new DailySummaryResponse.Data(
							request.getDate(),
							gptSummary.topKeywords,
							gptSummary.dailyTimeline,
							gptSummary.summaryText,
							new DailySummaryResponse.ActivityStats(
								stats.totalUsageMinutes,
								stats.getCategoryPercentages()
							)
						)
					));
			})
			.onErrorResume(e -> {
				log.error("일일 요약 생성 실패", e);
				return Mono.error(new RuntimeException("서버 오류: " + e.getMessage(), e));
			});
	}

	private Mono<List<CategorizedPage>> fetchCategoriesFromGPT(List<VisitedPageForTimeDto> pages) {
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
			return Mono.error(e);
		}

		Map<String, Object> body = Map.of(
			"model", OPENAI_MODEL,
			"messages", List.of(
				Map.of("role", "system", "content", "당신은 인터넷 기록 분류 전문가입니다."),
				Map.of("role", "user", "content", prompt)
			),
			"temperature", 0.2
		);

		return openAiWebClient.post()
			.uri(OPENAI_URI)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(body)
			.retrieve()
			.bodyToMono(Map.class)
			.flatMap(response -> {
				try {
					List<?> choices = (List<?>) response.get("choices");
					if (choices == null || choices.isEmpty()) {
						return Mono.error(new RuntimeException("GPT 응답에 choices가 없습니다."));
					}
					Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
					if (message == null) {
						return Mono.error(new RuntimeException("GPT 응답에 message가 없습니다."));
					}
					String content = Objects.toString(message.get("content"), "").trim();
					if (content.isEmpty()) {
						return Mono.error(new RuntimeException("GPT 응답 내용이 비어있습니다."));
					}
					List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

					if (parsedList.size() != pages.size()) {
						return Mono.error(new RuntimeException("카테고리 분류 결과 크기가 요청 데이터와 다릅니다."));
					}

					List<CategorizedPage> result = new ArrayList<>();
					for (int i = 0; i < pages.size(); i++) {
						VisitedPageForTimeDto page = pages.get(i);
						Map<String, String> catInfo = parsedList.get(i);
						String category = catInfo.get("category");
						if (category == null || category.isBlank()) {
							category = "분류불가";
						}
						result.add(new CategorizedPage(page, category));
					}
					return Mono.just(result);
				} catch (Exception e) {
					return Mono.error(new RuntimeException("GPT 응답 파싱 실패: " + e.getMessage(), e));
				}
			});
	}

	private DailyActivityStats calculateStats(List<CategorizedPage> pages) {
		int totalSeconds = 0;
		Map<String, Integer> categoryToSeconds = new HashMap<>();

		for (CategorizedPage page : pages) {
			int duration = page.page.getDurationSeconds();
			totalSeconds += duration;
			categoryToSeconds.put(page.category,
				categoryToSeconds.getOrDefault(page.category, 0) + duration);
		}
		int totalMinutes = totalSeconds / 60;

		return new DailyActivityStats(totalMinutes, categoryToSeconds);
	}

	private Mono<GptSummary> fetchDailySummaryFromGPT(String date, List<CategorizedPage> pages) {
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

		Map<String, Object> body = Map.of(
			"model", OPENAI_MODEL,
			"messages", List.of(
				Map.of("role", "system", "content", "당신은 친절한 일일 활동 요약 전문가입니다."),
				Map.of("role", "user", "content", prompt)
			),
			"temperature", 0.3
		);

		return openAiWebClient.post()
			.uri(OPENAI_URI)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(body)
			.retrieve()
			.bodyToMono(Map.class)
			.flatMap(response -> {
				try {
					List<?> choices = (List<?>) response.get("choices");
					if (choices == null || choices.isEmpty()) {
						return Mono.error(new RuntimeException("GPT 응답에 choices가 없습니다."));
					}
					Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
					if (message == null) {
						return Mono.error(new RuntimeException("GPT 응답에 message가 없습니다."));
					}
					String content = Objects.toString(message.get("content"), "").trim();
					if (content.isEmpty()) {
						return Mono.error(new RuntimeException("GPT 응답 내용이 비어있습니다."));
					}

					Map<String, Object> parsed = objectMapper.readValue(content, Map.class);

					// 파싱된 topKeywords
					List<Map<String, Object>> keywordsRaw = (List<Map<String, Object>>) parsed.get("topKeywords");
					List<DailySummaryResponse.TopKeyword> keywords = new ArrayList<>();
					if (keywordsRaw != null) {
						for (Map<String, Object> k : keywordsRaw) {
							keywords.add(new DailySummaryResponse.TopKeyword(
								Objects.toString(k.get("keyword"), ""),
								((Number) k.get("frequency")).intValue()
							));
						}
					}

					// 파싱된 dailyTimeline
					List<Map<String, Object>> timelineRaw = (List<Map<String, Object>>) parsed.get("dailyTimeline");
					List<DailySummaryResponse.DailyTimelineEntry> timeline = new ArrayList<>();
					if (timelineRaw != null) {
						for (Map<String, Object> t : timelineRaw) {
							timeline.add(new DailySummaryResponse.DailyTimelineEntry(
								Objects.toString(t.get("time"), ""),
								Objects.toString(t.get("description"), "")
							));
						}
					}

					// 파싱된 summaryText
					List<String> summaryText = (List<String>) parsed.get("summaryText");
					if (summaryText == null) {
						summaryText = List.of();
					}

					return Mono.just(new GptSummary(keywords, timeline, summaryText));
				} catch (Exception e) {
					return Mono.error(new RuntimeException("GPT 일일 요약 파싱 실패: " + e.getMessage(), e));
				}
			});
	}

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

		public List<DailySummaryResponse.ActivityProportion> getCategoryPercentages() {
			List<DailySummaryResponse.ActivityProportion> list = new ArrayList<>();
			if (totalUsageMinutes == 0) return list;

			for (Map.Entry<String, Integer> e : categorySeconds.entrySet()) {
				int min = e.getValue() / 60;
				int percent = (int) Math.round(min * 100.0 / totalUsageMinutes);
				list.add(new DailySummaryResponse.ActivityProportion(e.getKey(), percent));
			}
			return list;
		}
	}

	private static class GptSummary {
		List<DailySummaryResponse.TopKeyword> topKeywords;
		List<DailySummaryResponse.DailyTimelineEntry> dailyTimeline;
		List<String> summaryText;

		public GptSummary(List<DailySummaryResponse.TopKeyword> topKeywords,
			List<DailySummaryResponse.DailyTimelineEntry> dailyTimeline,
			List<String> summaryText) {
			this.topKeywords = topKeywords;
			this.dailyTimeline = dailyTimeline;
			this.summaryText = summaryText;
		}
	}

	// DTO - Response Record
	public static record DailySummaryResponse(
		int code,
		String msg,
		Data data
	) {
		public record Data(
			String date,
			List<TopKeyword> topKeywords,
			List<DailyTimelineEntry> dailyTimeline,
			List<String> summaryText,
			ActivityStats activityStats
		) {
		}

		public record TopKeyword(String keyword, int frequency) {
		}

		public record DailyTimelineEntry(String time, String description) {
		}

		public record ActivityStats(
			int totalUsageTimeMinutes,
			List<ActivityProportion> activityProportions
		) {
		}

		public record ActivityProportion(String category, int percentage) {
		}
	}
}
