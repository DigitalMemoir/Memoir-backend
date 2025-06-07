package com.univ.memoir.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.memoir.api.dto.req.VisitedPageDto;
import com.univ.memoir.api.dto.req.VisitedPagesRequest;
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
public class KeywordService {
    private static final Logger log = LoggerFactory.getLogger(KeywordService.class);

    private static final String OPENAI_MODEL = "gpt-3.5-turbo";
    private static final String OPENAI_URI = "/chat/completions";

    private final ObjectMapper objectMapper;
    private final WebClient openAiWebClient;

    public Mono<Map> analyzeKeywords(String accessToken, VisitedPagesRequest request) {
        List<VisitedPageDto> visitedPages = request.getVisitedPages();
        if (visitedPages == null || visitedPages.isEmpty()) {
            return Mono.just(Map.of("code", 400, "msg", "방문 페이지 데이터가 없습니다."));
        }

        String prompt;
        try {
            prompt = createPrompt(visitedPages);
        } catch (JsonProcessingException e) {
            log.error("프롬프트 생성 실패", e);
            return Mono.just(Map.of("code", 500, "msg", "프롬프트 생성 실패: " + e.getMessage()));
        }

        Map<String, Object> requestBody = Map.of(
                "model", OPENAI_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 인터넷 검색 기록을 보고 주요 키워드를 추출해주는 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        return openAiWebClient.post()
                .uri(OPENAI_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        List<?> choices = (List<?>) response.get("choices");
                        if (choices == null || choices.isEmpty()) {
                            log.warn("OpenAI 응답에 choices가 없습니다.");
                            return Map.of("code", 500, "msg", "OpenAI 응답에 choices가 없습니다.");
                        }

                        Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        String content = Objects.toString(message.get("content"), "");

                        // 응답이 JSON 형식이어야 하므로 파싱 시도
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

    private String createPrompt(List<VisitedPageDto> visitedPages) throws JsonProcessingException {
        String template = """
                아래 JSON은 사용자가 최근 방문한 페이지들의 데이터입니다.
                각 페이지 제목에서 의미 있는 핵심 키워드(명사, 주요 단어 등)를 추출해 주세요.
                이때 제목 하나 당 하나의 키워드를 추출해주세요.
                키워드는 중복 없이, 빈도수와 함께 계산해 주세요.

                응답은 반드시 아래 JSON 형식을 따르세요
                {
                  "keywordFrequencies": [
                    { "keyword": "", "frequency": 3 },
                    { "keyword": "키워드2", "frequency": 2 }
                  ]
                }
                %s
                """;
        return String.format(template, objectMapper.writeValueAsString(Map.of("visitedPages", visitedPages)));
    }
}
