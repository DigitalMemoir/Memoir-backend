package com.univ.memoir.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univ.memoir.api.dto.req.VisitedPageDto;
import com.univ.memoir.api.dto.req.VisitedPagesRequest;
import com.univ.memoir.api.dto.res.KeywordFrequencyDto;
import com.univ.memoir.api.dto.res.keyword.KeywordResponseDto;
import com.univ.memoir.core.domain.KeywordData;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.KeywordDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class KeywordService {

    private static final String OPENAI_MODEL = "gpt-3.5-turbo";
    private static final String OPENAI_URI = "https://api.openai.com/v1/chat/completions";

    private final ObjectMapper objectMapper;
    private final RestTemplate openAiRestTemplate;
    private final UserService userService;
    private final KeywordDataRepository keywordDataRepository;

    @Value("${openai.api.key}")
    private String apiKey;

    // 기존 analyzeKeywords 메서드는 변경 없음
    public KeywordResponseDto analyzeKeywords(String accessToken, VisitedPagesRequest request) {
        User user = userService.findByAccessToken(accessToken);
        List<VisitedPageDto> visitedPages = request.getVisitedPages();

        if (visitedPages == null || visitedPages.isEmpty()) {
            throw new IllegalArgumentException("방문 페이지 데이터가 없습니다.");
        }

        String prompt;
        try {
            prompt = createPrompt(visitedPages);
        } catch (JsonProcessingException e) {
            log.error("프롬프트 생성 실패", e);
            throw new RuntimeException("프롬프트 생성 실패: " + e.getMessage());
        }

        Map<String, Object> requestBody = Map.of(
                "model", OPENAI_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 인터넷 검색 기록을 보고 주요 키워드를 추출해주는 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = openAiRestTemplate.postForEntity(OPENAI_URI, entity, Map.class);

            List<?> choices = (List<?>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("OpenAI 응답에 choices가 없습니다.");
                throw new RuntimeException("OpenAI 응답에 choices가 없습니다.");
            }

            Map<String, Object> choice = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = Objects.toString(message.get("content"), "");

            // content → KeywordResponseDto 역직렬화
            KeywordResponseDto keywordResponse = objectMapper.readValue(content, KeywordResponseDto.class);

            // DB 저장 로직 (업데이트 기능 추가)
            saveOrUpdateKeywordsToDB(user, keywordResponse); // 메서드 이름 변경

            return keywordResponse;

        } catch (Exception e) {
            log.error("GPT 호출 또는 응답 파싱 실패", e);
            throw new RuntimeException("GPT 호출 또는 응답 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 새로운 키워드 데이터를 기존 키워드 데이터와 비교하여 업데이트하거나 새로 저장합니다.
     * 키워드는 대소문자를 구분하지 않고 비교하며, 양 끝의 공백을 제거합니다.
     *
     * @param user 현재 사용자
     * @param dto GPT로부터 받은 새로운 키워드 응답 DTO
     */
    @Transactional // 이 메서드는 DB 작업을 포함하므로 트랜잭션 관리가 필요합니다.
    protected void saveOrUpdateKeywordsToDB(User user, KeywordResponseDto dto) {
        if (dto.getKeywordFrequencies() == null || dto.getKeywordFrequencies().isEmpty()) {
            return;
        }

        List<KeywordData> existingKeywords = keywordDataRepository.findByUser(user);

        // 1. 기존 키워드 데이터를 정규화된 키워드 기준으로 그룹화하고 빈도수를 합칩니다.
        //    이렇게 하면 DB에 "서울 맛집"이 여러 개 있어도 하나의 KeywordData 엔티티로 통합됩니다.
        Map<String, KeywordData> consolidatedExistingKeywordMap = existingKeywords.stream()
                .collect(Collectors.toMap(
                        kw -> normalizeKeyword(kw.getKeyword()), // 정규화된 키워드를 키로
                        kw -> kw,                                // KeywordData 객체 자체를 값으로
                        (existing, replacement) -> {
                            log.warn("사용자 {}에 대해 중복된 키워드 '{}' (정규화된: '{}') 발견. 빈도수 합칩니다.",
                                    user.getId(), existing.getKeyword(), normalizeKeyword(existing.getKeyword()));
                            existing.setFrequency(existing.getFrequency() + replacement.getFrequency());
                            return existing;
                        }
                ));

        // 2. 만약 DB에 실제로 동일한 키워드인데 ID만 다른 여러 레코드가 있다면,
        //    위 consolidation 과정에서 Map에 하나만 남게 됩니다.
        //    따라서 이제 중복된 KeywordData 엔티티를 삭제해야 합니다.
        //    @Transactional이 이 메서드에 적용되어야 합니다.
        List<KeywordData> keywordsToRemove = existingKeywords.stream()
                .filter(kw -> !consolidatedExistingKeywordMap.containsValue(kw)) // Map에 포함되지 않은 (즉, 합쳐진) 엔티티들
                .collect(Collectors.toList());

        if (!keywordsToRemove.isEmpty()) {
            keywordDataRepository.deleteAll(keywordsToRemove);
            log.info("사용자 {}의 중복 키워드 {}개 제거 완료.", user.getId(), keywordsToRemove.size());
        }

        List<KeywordData> keywordsToPersist = new ArrayList<>(); // 저장 또는 업데이트할 엔티티 목록

        // 3. GPT 응답으로 받은 새로운 키워드를 순회합니다.
        for (KeywordFrequencyDto newKf : dto.getKeywordFrequencies()) {
            String normalizedNewKeyword = normalizeKeyword(newKf.getKeyword()); // 새 키워드 정규화
            KeywordData existingKeywordData = consolidatedExistingKeywordMap.get(normalizedNewKeyword); // 통합된 맵에서 조회

            if (existingKeywordData != null) {
                // 4. 기존 키워드가 존재하면 빈도수를 업데이트합니다.
                existingKeywordData.setFrequency(existingKeywordData.getFrequency() + newKf.getFrequency());
                keywordsToPersist.add(existingKeywordData); // 업데이트된 엔티티를 목록에 추가
            } else {
                // 5. 새로운 키워드이면 새로 생성하여 목록에 추가합니다.
                keywordsToPersist.add(new KeywordData(user, newKf.getKeyword(), newKf.getFrequency()));
            }
        }

        // 6. 변경된 모든 키워드 데이터를 한 번에 저장하거나 업데이트합니다.
        keywordDataRepository.saveAll(keywordsToPersist);
    }

    /**
     * 특정 사용자의 오늘 날짜에 해당하는 상위 9개 키워드를 빈도수 기준으로 반환합니다.
     * 키워드는 빈도수가 높은 순서로 정렬됩니다.
     *
     * @param accessToken 사용자 인증 토큰
     * @return 상위 9개 키워드 목록 DTO
     */
    public List<KeywordFrequencyDto> getTopKeywordsForToday(String accessToken) {
        User user = userService.findByAccessToken(accessToken);
        if (user == null) {
            throw new IllegalArgumentException("유효하지 않은 사용자 토큰입니다.");
        }

        // 오늘 날짜의 시작과 끝 시간을 KST 기준으로 설정
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kstZone);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX); // 23:59:59.999999999

        // KeywordDataRepository를 통해 해당 사용자의 오늘 날짜 키워드 조회
        // 이 메서드는 KeywordDataRepository에 추가되어야 합니다.
        List<KeywordData> todayKeywords = keywordDataRepository.findByUserAndCreatedAtBetween(
                user, startOfDay, endOfDay);

        // 키워드 빈도수를 합산 (동일 키워드가 여러 번 저장되었을 경우 대비)
        Map<String, Integer> combinedFrequencies = todayKeywords.stream()
                .collect(Collectors.groupingBy(
                        kw -> normalizeKeyword(kw.getKeyword()),
                        Collectors.summingInt(KeywordData::getFrequency)
                ));

        // KeywordFrequencyDto로 변환 후 빈도수 내림차순 정렬, 상위 9개만 선택
        return combinedFrequencies.entrySet().stream()
                .map(entry -> new KeywordFrequencyDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(KeywordFrequencyDto::getFrequency).reversed())
                .limit(9)
                .collect(Collectors.toList());
    }


    /**
     * 키워드 문자열을 정규화하여 대소문자를 구분하지 않고 양 끝의 공백을 제거합니다.
     *
     * @param keyword 정규화할 키워드
     * @return 정규화된 키워드 문자열
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim().toLowerCase();
    }

    private String createPrompt(List<VisitedPageDto> visitedPages) throws JsonProcessingException {
        String template = """
                아래 JSON은 사용자가 최근 방문한 페이지들의 데이터입니다.
                각 페이지 제목에서 의미 있는 핵심 키워드(명사, 주요 단어 등)를 추출해 주세요.
                이때 제목 하나 당 하나의 키워드를 추출해주세요.
                키워드는 중복 없이, 빈도수와 함께 계산해 주세요.

                응답은 반드시 아래 JSON 형식을 따르세요:
                {
                  "keywordFrequencies": [
                    { "keyword": "예시1", "frequency": 3 },
                    { "keyword": "예시2", "frequency": 2 }
                  ]
                }

                %s
                """;

        return String.format(template, objectMapper.writeValueAsString(Map.of("visitedPages", visitedPages)));
    }
}