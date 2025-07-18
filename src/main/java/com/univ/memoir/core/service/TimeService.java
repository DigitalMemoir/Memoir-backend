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

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.customException.UserNotFoundException;
import com.univ.memoir.core.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TimeService {
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userservice;

    public TimeService(@Qualifier("openAiRestTemplate") RestTemplate restTemplate, ObjectMapper objectMapper, UserService userservice) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userservice = userservice;
    }

    @Value("${openai.api.base-url}")
    private String openAIBaseUrl;

    @Value("${openai.uri}")
    private String openAiUri;

    @Value("${openai.model}")
    private String openAiModel;

    public ActivityStats analyzeTimeStats(String accessToken, TimeAnalysisRequest request) {
        User currentUser = userservice.findByAccessToken(accessToken);

        if (currentUser == null) {
            throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        List<VisitedPageForTimeDto> pages = request.getVisitedPages();
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("방문 기록이 없습니다.");
        }

        try {
            List<CategorizedPage> categorizedPages = fetchCategoriesFromGPT(pages);
            return calculateUsageStats(categorizedPages);
        } catch (Exception e) {
            log.error("사용 시간 분석 실패", e);
            throw new RuntimeException("서버 오류: " + e.getMessage(), e);
        }
    }

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
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 인터넷 기록 분류 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    openAIBaseUrl + openAiUri,
                    body,
                    Map.class
            );

            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("GPT 응답에 'choices' 필드가 없습니다.");
            }

            Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
            String content = Objects.toString(message.get("content"), "").trim();
            List<Map<String, String>> parsedList = objectMapper.readValue(content, List.class);

            if (parsedList.size() != pages.size()) {
                log.warn("GPT 응답 결과 크기가 요청 페이지 수와 다릅니다. 요청: {}개, 응답: {}개. GPT 응답 내용: {}", pages.size(), parsedList.size(), content);
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

    private ActivityStats calculateUsageStats(List<CategorizedPage> pages) {
        int totalSeconds = 0;
        Map<String, Integer> categoryToSeconds = new HashMap<>();
        Map<Integer, Map<String, Integer>> hourlyCategoryMinutes = new TreeMap<>();

        for (CategorizedPage page : pages) {
            String category = page.category;
            int duration = page.page.getDurationSeconds();
            long startTimestamp = page.page.getStartTimestamp();
            totalSeconds += duration;

            categoryToSeconds.put(category, categoryToSeconds.getOrDefault(category, 0) + duration);

            long currentTimestamp = startTimestamp;
            int remainingDuration = duration;

            while (remainingDuration > 0) {
                ZonedDateTime currentZdt = Instant.ofEpochSecond(currentTimestamp).atZone(ZoneId.of("Asia/Seoul"));
                int currentHour = currentZdt.getHour();

                ZonedDateTime endOfCurrentHour = currentZdt
                        .withMinute(59)
                        .withSecond(59)
                        .withNano(0);

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

        List<CategorySummary> categorySummaries = categoryToSeconds.entrySet().stream()
                .map(e -> new CategorySummary(e.getKey(), e.getValue() / 60))
                .sorted(Comparator.comparing(CategorySummary::getTotalTimeMinutes).reversed())
                .collect(Collectors.toList());

        List<HourlyBreakdown> hourlyBreakdowns = hourlyCategoryMinutes.entrySet().stream()
                .map(entry -> {
                    int hour = entry.getKey();
                    Map<String, Integer> categoryMinutes = entry.getValue();
                    int totalMinutesForHour = categoryMinutes.values().stream().mapToInt(Integer::intValue).sum();
                    return new HourlyBreakdown(hour, totalMinutesForHour, categoryMinutes);
                })
                .sorted(Comparator.comparing(HourlyBreakdown::getHour))
                .collect(Collectors.toList());

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