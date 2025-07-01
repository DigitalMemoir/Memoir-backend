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

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class TimeService {
    // 로그 출력을 위한 Logger
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);

    // OpenAI API에 사용할 모델과 URI 상수
    private static final String OPENAI_MODEL = "gpt-3.5-turbo";
    private static final String OPENAI_URI = "/chat/completions";

    // 의존성 주입
    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 방문 기록을 기반으로 GPT에게 사용 시간 분석 요청을 보내고 결과를 반환
     */
    public Mono<Map> analyzeTimeStats(String accessToken, TimeAnalysisRequest request) {
        List<VisitedPageForTimeDto> visitedPages = request.getVisitedPages();
        if (visitedPages == null || visitedPages.isEmpty()) {
            return Mono.just(Map.of("code", 400, "msg", "방문 기록이 없습니다."));
        }

        // GPT 프롬프트 생성
        String prompt;
        try {
            prompt = createPrompt(request);
        } catch (JsonProcessingException e) {
            log.error("프롬프트 생성 실패", e);
            return Mono.just(Map.of("code", 500, "msg", "프롬프트 생성 실패"));
        }

        // OpenAI API에 요청할 body 구성
        Map<String, Object> body = Map.of(
                "model", OPENAI_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 인터넷 검색 기록을 보고 사용 시간과 분포를 알려주는 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        // WebClient를 통해 GPT 호출
        return openAiWebClient.post()
                .uri(OPENAI_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        List<?> choices = (List<?>) response.get("choices");
                        if (choices == null || choices.isEmpty()) {
                            log.warn("GPT 응답에 choices가 없습니다.");
                            return Map.of("code", 500, "msg", "GPT 응답에 choices가 없습니다.");
                        }

                        // 첫 번째 choice에서 message content 추출
                        Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        String content = Objects.toString(message.get("content"), "");

                        // content는 JSON 문자열이므로 Map으로 파싱 후 반환
                        return objectMapper.readValue(content, Map.class);
                    } catch (Exception e) {
                        log.error("GPT 응답 파싱 실패", e);
                        return Map.of("code", 500, "msg", "GPT 응답 파싱 실패: " + e.getMessage());
                    }
                })
                .onErrorResume(e -> {
                    log.error("GPT 호출 실패", e);
                    return Mono.just(Map.of("code", 500, "msg", "GPT 호출 실패: " + e.getMessage()));
                });
    }

    /**
     * 사용자의 방문 기록을 기반으로 GPT 프롬프트 문자열 생성
     */
    private String createPrompt(TimeAnalysisRequest request) throws JsonProcessingException {
        String template = """
                아래는 사용자가 %s 하루 동안 방문한 웹 페이지 기록입니다.
                각 기록은 제목, 방문 횟수, 시작 시간 (유닉스 타임스탬프), 사용 시간(초)을 포함합니다.

                당신의 임무:
                1. 페이지 제목과 URL을 참고하여, 각 페이지의 카테고리를 판단하세요.
                   ('공부', '뉴스 탐색', '콘텐츠 소비', '쇼핑', '업무/프로젝트' 중 하나로 )
                2. 카테고리별 총 사용 시간을 분 단위로 합산하세요.
                3. 시간대별(hour 단위 0~23시)로 사용 시간을 나누어 정리하세요. 모든 시간대에 대해 반드시 결과를 포함하며, 사용량이 없으면 0으로 표시하세요.
                4. 아래 형식으로 응답하세요 (JSON strict):

                {
                  "activityStats": {
                    "totalUsageTimeMinutes": 360, 
                    "categorySummaries": [
                      { "category": "공부, 학습", "totalTimeMinutes": 180 },
                      { "category": "뉴스, 정보 탐색", "totalTimeMinutes": 90 }, 
                      { "category": "콘텐츠 소비", "totalTimeMinutes": 60 }, 
                      { "category": "쇼핑", "totalTimeMinutes": 30 },    
                      { "category": "업무, 프로젝트", "totalTimeMinutes": 0 }
                    ],
                    "hourlyActivityBreakdown": [
                        {
                          "hour": 6, // 오전 6시
                          "totalUsageMinutes": 30,
                          "categoryMinutes": { "뉴스, 정보 탐색": 30, "업무, 프로젝트": 0, "공부, 학습": 0, "콘텐츠 소비": 0, "쇼핑": 0 }
                        },
                        {
                          "hour": 7,
                          "totalUsageMinutes": 0,
                          "categoryMinutes": { "업무, 프로젝트": 0, "공부, 학습": 0, "뉴스, 정보 탐색": 0, "콘텐츠 소비": 0, "쇼핑": 0 }
                        },
                        {
                          "hour": 8, // 오전 8시
                          "totalUsageMinutes": 60,
                          "categoryMinutes": { "공부, 학습": 60, "업무, 프로젝트": 0, "뉴스, 정보 탐색": 0, "콘텐츠 소비": 0, "쇼핑": 0 }
                        }
                    ]
                  }
                }
                """;

        // 방문 기록을 JSON 문자열로 변환
        String json = objectMapper.writeValueAsString(Map.of("visitedPages", request.getVisitedPages()));

        // 템플릿에 날짜 삽입 후, JSON 데이터를 이어붙여 최종 프롬프트 구성
        return String.format(template, request.getDate()) + "\n" + json;
    }
}