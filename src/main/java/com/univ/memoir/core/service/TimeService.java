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
import org.springframework.web.client.RestTemplate; // WebClient 대신 RestTemplate 사용

@Service
@RequiredArgsConstructor
public class TimeService {
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);

    private final RestTemplate restTemplate; // WebClient 대신 RestTemplate 주입
    private final ObjectMapper objectMapper;

    private static final String OPENAI_MODEL = "gpt-3.5-turbo";
    private static final String OPENAI_URI = "/chat/completions";
    private static final String OPENAI_BASE_URL = "YOUR_OPENAI_API_BASE_URL"; // OpenAI API 기본 URL 설정 필요

    /**
     * 사용자의 웹 활동 시간을 분석하여 통계를 반환합니다.
     *
     * @param accessToken 사용자 인증 토큰 (현재 사용되지 않음)
     * @param request 시간 분석 요청 DTO
     * @return 분석된 활동 통계
     * @throws IllegalArgumentException 방문 기록이 없는 경우
     * @throws RuntimeException GPT 통신 또는 데이터 처리 중 오류 발생 시
     */
    public ActivityStats analyzeTimeStats(String accessToken, TimeAnalysisRequest request) {
        List<VisitedPageForTimeDto> pages = request.getVisitedPages();
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("방문 기록이 없습니다.");
        }

        try {
            // 1. GPT를 통해 페이지 카테고리 분류 (동기 호출)
            List<CategorizedPage> categorizedPages = fetchCategoriesFromGPT(pages);

            // 2. 분류된 페이지를 기반으로 사용 통계 계산
            return calculateUsageStats(categorizedPages);
        } catch (Exception e) {
            log.error("사용 시간 분석 실패", e);
            throw new RuntimeException("서버 오류: " + e.getMessage(), e);
        }
    }

    /**
     * GPT를 호출하여 방문 페이지의 카테고리를 분류합니다.
     *
     * @param pages 방문 페이지 목록
     * @return 카테고리가 분류된 페이지 목록
     * @throws RuntimeException GPT 통신 또는 응답 파싱 중 오류 발생 시
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

        Map<String, Object> body = Map.of(
                "model", OPENAI_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 인터넷 기록 분류 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        try {
            // RestTemplate을 이용한 동기 HTTP POST 요청
            Map<String, Object> response = restTemplate.postForObject(
                    OPENAI_BASE_URL + OPENAI_URI,
                    body,
                    Map.class
            );

            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("GPT 응답에 'choices' 필드가 없습니다.");
            }

            Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
            String content = Objects.toString(message.get("content"), "").trim(); // trim() 추가하여 공백 제거
            List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

            // GPT가 모든 페이지에 대해 응답하지 않거나 순서가 섞일 수 있으므로
            // 원래 pages와 parsedList의 크기를 비교하고, 매핑 방식에 주의해야 합니다.
            // 현재 코드에서는 인덱스를 통해 매핑하는데, GPT가 다른 순서로 반환할 가능성도 있습니다.
            // 더 견고하게 하려면 title/url 매칭 로직을 추가하는 것이 좋습니다.
            if (parsedList.size() != pages.size()) {
                log.warn("GPT 응답 결과 크기가 요청 페이지 수와 다릅니다. 요청: {}개, 응답: {}개. GPT 응답 내용: {}", pages.size(), parsedList.size(), content);
                // 이 경우 오류를 던지거나 부분적으로만 처리할지 결정해야 합니다.
                // 여기서는 일단 오류를 던지도록 하겠습니다.
                throw new RuntimeException("GPT 카테고리 분류 결과 크기가 요청 데이터와 다릅니다.");
            }

            List<CategorizedPage> result = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                VisitedPageForTimeDto page = pages.get(i);
                Map<String, String> catInfo = parsedList.get(i);
                result.add(new CategorizedPage(page, catInfo.get("category")));
            }
            return result;
        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패", e);
            throw new RuntimeException("GPT 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 카테고리가 분류된 페이지 목록을 기반으로 사용 통계를 계산합니다.
     * 총 사용 시간, 카테고리별 요약, 시간대별 사용량 분석을 포함합니다.
     *
     * @param pages 카테고리가 분류된 페이지 목록
     * @return 계산된 활동 통계
     */
    private ActivityStats calculateUsageStats(List<CategorizedPage> pages) {
        int totalSeconds = 0;
        Map<String, Integer> categoryToSeconds = new HashMap<>();
        Map<Integer, Map<String, Integer>> hourlyCategoryMinutes = new TreeMap<>(); // 시간대별 카테고리 사용 분

        for (CategorizedPage page : pages) {
            String category = page.category;
            int duration = page.page.getDurationSeconds();
            long startTimestamp = page.page.getStartTimestamp();
            totalSeconds += duration;

            categoryToSeconds.put(category, categoryToSeconds.getOrDefault(category, 0) + duration);

            // 시간대별 사용량 계산
            long currentTimestamp = startTimestamp;
            int remainingDuration = duration;

            while (remainingDuration > 0) {
                ZonedDateTime currentZdt = Instant.ofEpochSecond(currentTimestamp).atZone(ZoneId.of("Asia/Seoul"));
                int currentHour = currentZdt.getHour();

                // 현재 시간의 끝 (59분 59초)까지 남은 시간 계산
                ZonedDateTime endOfCurrentHour = currentZdt
                        .withMinute(59)
                        .withSecond(59)
                        .withNano(0); // 나노초는 0으로 설정하여 정확히 시간 끝을 맞춤

                long secondsUntilEndOfHour = endOfCurrentHour.toEpochSecond() - currentTimestamp + 1;
                int secondsToRecordThisSegment = (int) Math.min(remainingDuration, secondsUntilEndOfHour);

                int minutesThisSegment = (int) Math.ceil(secondsToRecordThisSegment / 60.0);

                hourlyCategoryMinutes.putIfAbsent(currentHour, new HashMap<>());
                Map<String, Integer> catMap = hourlyCategoryMinutes.get(currentHour);
                catMap.put(category, catMap.getOrDefault(category, 0) + minutesThisSegment);

                remainingDuration -= secondsToRecordThisSegment;
                currentTimestamp += secondsToRecordThisSegment;
            }
        }

        // CategorySummary 리스트 생성
        List<CategorySummary> categorySummaries = categoryToSeconds.entrySet().stream()
                .map(e -> new CategorySummary(e.getKey(), e.getValue() / 60)) // 초를 분으로 변환
                .sorted(Comparator.comparing(CategorySummary::getTotalTimeMinutes).reversed()) // 사용 시간 내림차순 정렬
                .collect(Collectors.toList());

        // HourlyBreakdown 리스트 생성
        List<HourlyBreakdown> hourlyBreakdowns = hourlyCategoryMinutes.entrySet().stream()
                .map(entry -> {
                    int hour = entry.getKey();
                    Map<String, Integer> categoryMinutes = entry.getValue();
                    int totalMinutesForHour = categoryMinutes.values().stream().mapToInt(Integer::intValue).sum();
                    return new HourlyBreakdown(hour, totalMinutesForHour, categoryMinutes);
                })
                .sorted(Comparator.comparing(HourlyBreakdown::getHour)) // 시간(hour) 오름차순 정렬
                .collect(Collectors.toList());

        return new ActivityStats(totalSeconds / 60, categorySummaries, hourlyBreakdowns);
    }

    /**
     * GPT 분류 결과를 담는 내부 도우미 클래스.
     */
    static class CategorizedPage {
        VisitedPageForTimeDto page;
        String category;

        public CategorizedPage(VisitedPageForTimeDto page, String category) {
            this.page = page;
            this.category = category;
        }
    }
}