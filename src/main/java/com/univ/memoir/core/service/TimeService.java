package com.univ.memoir.core.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.univ.memoir.core.domain.TimeAnalysisData;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.TimeAnalysisDataRepository;

@Service
public class TimeService {
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final TimeAnalysisDataRepository timeAnalysisRepository;

    @Value("${openai.api.base-url}")
    private String openAIBaseUrl;

    @Value("${openai.uri}")
    private String openAiUri;

    @Value("${openai.model}")
    private String openAiModel;

    public TimeService(@Qualifier("openAiRestTemplate") RestTemplate restTemplate,
                       ObjectMapper objectMapper,
                       UserService userService,
                       TimeAnalysisDataRepository timeAnalysisRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.timeAnalysisRepository = timeAnalysisRepository;
    }

    /**
     * 시간 통계 분석
     *
     * @param email 사용자 이메일 (SecurityContext에서 추출)
     * @param request 시간 분석 요청 데이터
     * @return 활동 통계
     */
    public ActivityStats analyzeTimeStats(String email, TimeAnalysisRequest request) {
        User currentUser = userService.findByEmailForSummary(email);

        if (currentUser == null) {
            throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDate requestDate = LocalDate.parse(request.getDate());

        // GPT API 호출 후 저장
        log.info("Calling GPT API for user: {}, date: {}", currentUser.getId(), requestDate);
        List<VisitedPageForTimeDto> pages = request.getVisitedPages();
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("방문 기록이 없습니다.");
        }

        try {
            List<CategorizedPage> categorizedPages = fetchCategorizedPages(pages);
            ActivityStats result = summarizeActivity(categorizedPages);

            // DB에 저장
            saveToDatabase(currentUser, requestDate, result);

            return result;
        } catch (Exception e) {
            log.error("시간 분석 실패", e);
            throw new RuntimeException("서버 오류: " + e.getMessage(), e);
        }
    }

    private void saveToDatabase(User user, LocalDate date, ActivityStats stats) {
        try {
            TimeAnalysisData data = new TimeAnalysisData(
                    user,
                    date,
                    stats.getTotalUsageTimeMinutes(),
                    objectMapper.writeValueAsString(stats.getCategorySummaries()),
                    objectMapper.writeValueAsString(stats.getHourlyActivityBreakdown())
            );
            timeAnalysisRepository.save(data);
            log.debug("Time analysis data saved - userId: {}, date: {}", user.getId(), date);
        } catch (Exception e) {
            log.error("Failed to save time analysis data", e);
            // 저장 실패해도 결과는 반환
        }
    }

    private ActivityStats convertToActivityStats(TimeAnalysisData data) {
        try {
            List<CategorySummary> categorySummaries = objectMapper.readValue(
                    data.getCategorySummariesJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CategorySummary.class)
            );

            List<HourlyBreakdown> hourlyBreakdowns = objectMapper.readValue(
                    data.getHourlyBreakdownsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HourlyBreakdown.class)
            );

            return new ActivityStats(data.getTotalUsageMinutes(), categorySummaries, hourlyBreakdowns);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("시간 분석 데이터 역직렬화 실패", e);
        }
    }

    // ===== 기존 메서드들 (GPT API 호출 관련) =====

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
            아래는 사용자의 방문 기록입니다. 각 페이지의 제목과 URL을 분석하여 정확한 카테고리를 분류해주세요.
            
            **카테고리 분류 기준:**
            - '공부, 학습': GitHub, Stack Overflow, 기술 문서, 온라인 강의, 코딩 문제 사이트
            - '뉴스, 정보 탐색': 뉴스 사이트, 기술 뉴스, LinkedIn 등
            - '콘텐츠 소비': 유튜브, 넷플릭스, 인스타그램, 트위터 등 SNS/미디어
            - '쇼핑': 쿠팡, 11번가, 아마존 등 쇼핑몰
            - '업무, 프로젝트': Google Docs, Notion, Slack, Jira, AWS 콘솔 등
            
            **중요: 반드시 위 5개 카테고리 중 하나로만 분류하고, 빈 값이나 다른 값을 사용하지 마세요.**
            
            JSON 배열로만 응답하세요. 설명이나 추가 텍스트는 절대 포함하지 마세요:
            [
              { "title": "페이지 제목", "url": "URL", "category": "정확한 카테고리명" }
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

        // ✅ 마크다운 코드블록 제거
        content = cleanJsonContent(content);

        log.info("Raw GPT response: {}", Objects.toString(message.get("content"), "").trim());
        log.info("Cleaned GPT response content: {}", content);

        try {
            List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

            // 파싱된 결과 검증
            for (int i = 0; i < parsedList.size(); i++) {
                Map<String, String> item = parsedList.get(i);
                if (item == null || !item.containsKey("category")) {
                    // 잘못된 아이템 수정
                    parsedList.set(i, Map.of("title", "", "url", "", "category", DEFAULT_CATEGORY));
                }
            }

            // 누락된 응답을 기본 카테고리로 채우기
            while (parsedList.size() < originalSize) {
                parsedList.add(Map.of("title", "", "url", "", "category", DEFAULT_CATEGORY));
            }

            // 초과된 응답 제거
            if (parsedList.size() > originalSize) {
                parsedList = parsedList.subList(0, originalSize);
            }

            log.info("성공적으로 파싱됨. 결과 개수: {}", parsedList.size());
            return parsedList;

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패. 전체를 기본값으로 대체. Error: {}, Content: {}",
                    e.getMessage(), content.length() > 200 ? content.substring(0, 200) + "..." : content);

            // 파싱 실패 시 모든 페이지를 기본 카테고리로 설정
            List<Map<String, String>> fallbackList = new ArrayList<>();
            for (int i = 0; i < originalSize; i++) {
                fallbackList.add(Map.of("title", "", "url", "", "category", DEFAULT_CATEGORY));
            }
            return fallbackList;
        } catch (Exception e) {
            log.error("예상치 못한 에러 발생. 기본값으로 대체", e);

            // 예상치 못한 에러 시에도 기본값 반환
            List<Map<String, String>> fallbackList = new ArrayList<>();
            for (int i = 0; i < originalSize; i++) {
                fallbackList.add(Map.of("title", "", "url", "", "category", DEFAULT_CATEGORY));
            }
            return fallbackList;
        }
    }

    /**
     * GPT 응답에서 마크다운 코드블록과 불필요한 텍스트 제거
     */
    private String cleanJsonContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "[]";  // 빈 응답 처리
        }

        // 0. 기본 정제
        content = content.trim();

        // 1. ```json ... ``` 형태의 코드블록 제거
        if (content.contains("```")) {
            int start = content.indexOf("```");
            if (start != -1) {
                start = content.indexOf("\n", start);
                if (start == -1) start = content.indexOf("```") + 3;
                else start += 1;

                int end = content.lastIndexOf("```");
                if (end > start) {
                    content = content.substring(start, end).trim();
                }
            }
        }

        // 2. JSON 시작점 찾기
        int jsonStart = content.indexOf("[");
        if (jsonStart == -1) {
            log.warn("JSON 배열 시작점을 찾을 수 없음. 빈 배열 반환");
            return "[]";
        }

        // 3. JSON 끝점 찾기
        int jsonEnd = content.lastIndexOf("]");
        if (jsonEnd == -1 || jsonEnd <= jsonStart) {
            log.warn("JSON 배열 끝점을 찾을 수 없음. 빈 배열 반환");
            return "[]";
        }

        // 4. JSON 부분만 추출
        content = content.substring(jsonStart, jsonEnd + 1);

        // 5. 문제가 되는 문자들 정제
        content = content
                .replaceAll("[\u0000-\u001F\u007F-\u009F]", "") // 제어문자 제거
                .replaceAll("\\\\+", "\\\\")  // 연속된 백슬래시 정리
                .replaceAll("\\s+", " ")      // 연속된 공백 정리
                .trim();

        // 6. 빈 JSON 처리
        if (content.equals("[]") || content.isEmpty()) {
            return "[]";
        }

        return content;
    }

    private static final List<String> VALID_CATEGORIES = List.of(
            "공부, 학습", "뉴스, 정보 탐색", "콘텐츠 소비", "쇼핑", "업무, 프로젝트"
    );

    private static final String DEFAULT_CATEGORY = "콘텐츠 소비";

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

        // 최종 분 단위로 변환
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