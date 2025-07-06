package com.univ.memoir.core.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.memoir.api.dto.req.time.TimeAnalysisRequest;
import com.univ.memoir.api.dto.req.time.VisitedPageForTimeDto;
import com.univ.memoir.api.dto.res.time.ActivityStats;
import com.univ.memoir.api.dto.res.time.CategorySummary;
import com.univ.memoir.api.dto.res.time.HourlyBreakdown;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TimeService {
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_MODEL = "gpt-3.5-turbo";
    private static final String OPENAI_URI = "/chat/completions";

    public Mono<ActivityStats> analyzeTimeStats(String accessToken, TimeAnalysisRequest request) {
        List<VisitedPageForTimeDto> pages = request.getVisitedPages();
        if (pages == null || pages.isEmpty()) {
            return Mono.error(new IllegalArgumentException("방문 기록이 없습니다."));
        }

        return fetchCategoriesFromGPT(pages)
            .map(this::calculateUsageStats)
            .onErrorResume(e -> {
                log.error("사용 시간 분석 실패", e);
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
            .map(response -> {
                try {
                    List<?> choices = (List<?>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
                    String content = Objects.toString(message.get("content"), "");
                    List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

                    List<CategorizedPage> result = new ArrayList<>();
                    for (int i = 0; i < pages.size(); i++) {
                        VisitedPageForTimeDto page = pages.get(i);
                        Map<String, String> catInfo = parsedList.get(i);
                        result.add(new CategorizedPage(page, catInfo.get("category")));
                    }
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("GPT 응답 파싱 실패: " + e.getMessage(), e);
                }
            });
    }

    private ActivityStats calculateUsageStats(List<CategorizedPage> pages) {
        int totalSeconds = 0;
        Map<String, Integer> categoryToSeconds = new HashMap<>();
        Map<Integer, Map<String, Integer>> hourlyCategoryMinutes = new TreeMap<>();

        for (CategorizedPage page : pages) {
            String category = page.category;
            int duration = page.page.getDurationSeconds();
            long start = page.page.getStartTimestamp();
            totalSeconds += duration;

            categoryToSeconds.put(category, categoryToSeconds.getOrDefault(category, 0) + duration);

            int remaining = duration;
            while (remaining > 0) {
                ZonedDateTime zdt = Instant.ofEpochSecond(start).atZone(ZoneId.of("Asia/Seoul"));
                int hour = zdt.getHour();
                ZonedDateTime endOfHour = zdt.withMinute(59).withSecond(59);
                long endSec = endOfHour.toEpochSecond();
                int secondsThisHour = (int) Math.min(remaining, endSec - start + 1);

                int minutes = (int) Math.ceil(secondsThisHour / 60.0);
                hourlyCategoryMinutes.putIfAbsent(hour, new HashMap<>());
                Map<String, Integer> catMap = hourlyCategoryMinutes.get(hour);
                catMap.put(category, catMap.getOrDefault(category, 0) + minutes);

                remaining -= secondsThisHour;
                start += secondsThisHour;
            }
        }

        List<CategorySummary> categorySummaries = categoryToSeconds.entrySet().stream()
            .map(e -> new CategorySummary(e.getKey(), e.getValue() / 60))
            .collect(Collectors.toList());

        List<HourlyBreakdown> hourlyBreakdowns = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Integer>> entry : hourlyCategoryMinutes.entrySet()) {
            int hour = entry.getKey();
            Map<String, Integer> categoryMinutes = entry.getValue();
            int total = categoryMinutes.values().stream().mapToInt(Integer::intValue).sum();

            hourlyBreakdowns.add(new HourlyBreakdown(hour, total, categoryMinutes));
        }

        return new ActivityStats(totalSeconds / 60, categorySummaries, hourlyBreakdowns);
    }

    static class CategorizedPage {
        VisitedPageForTimeDto page;
        String category;

        public CategorizedPage(VisitedPageForTimeDto page, String category) {
            this.page = page;
            this.category = category;
        }
    }
}
