package com.univ.memoir.core.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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
import com.univ.memoir.api.dto.res.time.ActivityStats;
import com.univ.memoir.api.dto.res.time.CategorySummary;
import com.univ.memoir.api.dto.res.time.HourlyBreakdown;
import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.customException.UserNotFoundException;
import com.univ.memoir.core.domain.User;
@Service
public class TimeService {
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Value("${openai.api.base-url}")
    private String openAIBaseUrl;

    @Value("${openai.uri}")
    private String openAiUri;

    @Value("${openai.model}")
    private String openAiModel;

    public TimeService(@Qualifier("openAiRestTemplate") RestTemplate restTemplate,
        ObjectMapper objectMapper,
        UserService userService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    public ActivityStats analyzeTimeStats(String accessToken, TimeAnalysisRequest request) {
        User currentUser = userService.findByAccessToken(accessToken);
        if (currentUser == null) throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);

        List<VisitedPageForTimeDto> pages = request.getVisitedPages();
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("방문 기록이 없습니다.");
        }

        try {
            List<CategorizedPage> categorizedPages = fetchCategorizedPages(pages);
            return summarizeActivity(categorizedPages);
        } catch (Exception e) {
            log.error("시간 분석 실패", e);
            throw new RuntimeException("서버 오류: " + e.getMessage(), e);
        }
    }

    private List<CategorizedPage> fetchCategorizedPages(List<VisitedPageForTimeDto> pages) throws JsonProcessingException {
        String prompt = buildPrompt(pages);
        Map<String, Object> requestBody = buildChatRequest(prompt);

        Map<String, Object> response = restTemplate.postForObject(
            openAIBaseUrl + openAiUri, requestBody, Map.class);

        List<Map<String, String>> parsedList = extractCategorizedPages(response, pages.size());
        return mergePagesWithCategories(pages, parsedList);
    }

    private String buildPrompt(List<VisitedPageForTimeDto> pages) throws JsonProcessingException {
        String pagesJson = objectMapper.writeValueAsString(pages);
        return """
            아래는 사용자의 방문 기록입니다. 각 페이지의 제목과 URL을 참고하여 해당 페이지의 카테고리를 분류하세요.
            카테고리는 다음 중 하나로만 정하세요:
            '공부, 학습', '뉴스, 정보 탐색', '콘텐츠 소비', '쇼핑', '업무, 프로젝트'
                        
            단, 유튜브나 넷플릭스는 '콘텐츠 소비', Google Docs, notion 등은 '업무, 프로젝트', 뉴스 기사 사이트는 '뉴스, 정보 탐색', 쇼핑몰은 '쇼핑', 나머지 학습 목적 웹사이트는 '공부, 학습'으로 분류해 주세요.
                        

            다음 형식으로만 응답하세요 (JSON strict array):
            [
              { "title": "...", "url": "...", "category": "..." },
              ...
            ]

            방문 기록:
            %s
            """.formatted(pagesJson);
    }

    private Map<String, Object> buildChatRequest(String prompt) {
        return Map.of(
            "model", openAiModel,
            "messages", List.of(
                Map.of("role", "system", "content", "당신은 인터넷 기록 분류 전문가입니다."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.2
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractCategorizedPages(Map<String, Object> response, int originalSize) throws JsonProcessingException {
        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("GPT 응답에 'choices'가 없습니다.");
        }

        Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
        String content = Objects.toString(message.get("content"), "").trim();

        List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

        // 누락된 응답을 빈 카테고리로 채우기
        while (parsedList.size() < originalSize) {
            parsedList.add(Map.of("title", "", "url", "", "category", ""));
        }

        return parsedList;
    }

    private static final List<String> VALID_CATEGORIES = List.of(
        "공부, 학습", "뉴스, 정보 탐색", "콘텐츠 소비", "쇼핑", "업무, 프로젝트"
    );

    private static final String DEFAULT_CATEGORY = "콘텐츠 소비"; // 원하는 기본값으로 설정 가능

    private List<CategorizedPage> mergePagesWithCategories(List<VisitedPageForTimeDto> pages,
        List<Map<String, String>> categories) {
        List<CategorizedPage> result = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            String category = categories.get(i).get("category");

            // 비어 있거나 유효하지 않은 경우, 기본값으로 설정
            if (category == null || !VALID_CATEGORIES.contains(category.trim())) {
                log.warn("잘못된 카테고리 '{}' → 기본값 '{}'으로 대체", category, DEFAULT_CATEGORY);
                category = DEFAULT_CATEGORY;
            }

            result.add(new CategorizedPage(pages.get(i), category));
        }
        return result;
    }

    private ActivityStats summarizeActivity(List<CategorizedPage> pages) {
        int totalSeconds = 0;
        Map<String, Integer> categoryToSeconds = new HashMap<>();
        Map<Integer, Map<String, Integer>> hourlyCategorySeconds = new TreeMap<>();

        for (CategorizedPage page : pages) {
            int duration = page.page.getDurationSeconds();
            long start = page.page.getStartTimestamp();
            String category = page.category;
            totalSeconds += duration;

            categoryToSeconds.merge(category, duration, Integer::sum);
            distributeTimeAcrossHours(start, duration, category, hourlyCategorySeconds);
        }

        // 콘텐츠 소비 비율 재분배 로직 (초 단위로 조정)
        int contentConsumptionSeconds = categoryToSeconds.getOrDefault("콘텐츠 소비", 0);

        if (contentConsumptionSeconds > totalSeconds * 0.8) {
            int redistribute = contentConsumptionSeconds - (int)(totalSeconds * 0.7);
            contentConsumptionSeconds -= redistribute;

            categoryToSeconds.put("콘텐츠 소비", contentConsumptionSeconds);
            categoryToSeconds.merge("공부, 학습", redistribute / 2, Integer::sum);
            categoryToSeconds.merge("뉴스, 정보 탐색", redistribute / 2, Integer::sum);
        }

        // 📦 최종 분 단위로 변환
        List<CategorySummary> categorySummaries = categoryToSeconds.entrySet().stream()
            .map(e -> new CategorySummary(e.getKey(), e.getValue() / 60))
            .sorted(Comparator.comparing(CategorySummary::getTotalTimeMinutes).reversed())
            .collect(Collectors.toList());

        List<HourlyBreakdown> hourlyBreakdowns = hourlyCategorySeconds.entrySet().stream()
            .map(entry -> {
                Map<String, Integer> categoryMinutes = entry.getValue().entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() / 60 // 초 → 분
                    ));
                int hourTotalMinutes = categoryMinutes.values().stream().mapToInt(Integer::intValue).sum();
                return new HourlyBreakdown(entry.getKey(), hourTotalMinutes, categoryMinutes);
            })
            .collect(Collectors.toList());

        return new ActivityStats(totalSeconds / 60, categorySummaries, hourlyBreakdowns);
    }

    private void distributeTimeAcrossHours(long startTimestampMillis, int durationSeconds, String category,
        Map<Integer, Map<String, Integer>> hourlyCategorySeconds) {

        long currentTimeMillis = startTimestampMillis;
        int remaining = durationSeconds;

        while (remaining > 0) {
            ZonedDateTime current = Instant.ofEpochMilli(currentTimeMillis).atZone(ZoneId.of("Asia/Seoul"));
            int hour = current.getHour();

            ZonedDateTime endOfHour = current.withMinute(59).withSecond(59).withNano(999_000_000);
            long secondsUntilEnd = endOfHour.toEpochSecond() - current.toEpochSecond() + 1;

            int segment = (int) Math.min(remaining, secondsUntilEnd);

            hourlyCategorySeconds
                .computeIfAbsent(hour, h -> new HashMap<>())
                .merge(category, segment, Integer::sum);

            remaining -= segment;
            currentTimeMillis += segment * 1000L;
        }
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