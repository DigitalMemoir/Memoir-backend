package com.univ.memoir.core.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

/**
 * 키워드 분석 서비스 - 성능 최적화 버전
 * 기존 API 구조는 유지하면서 성능만 개선
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class KeywordService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final long CACHE_DURATION_MS = 6 * 60 * 60 * 1000L; // 6시간 (더 짧게)

    private final ObjectMapper objectMapper;
    private final RestTemplate openAiRestTemplate;
    private final UserService userService;
    private final KeywordDataRepository keywordDataRepository;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Value("${openai.api.base-url}${openai.uri}")
    private String openaiUri;

    /**
     * 성능 최적화된 메모리 캐시
     * 더 짧은 캐시 주기로 메모리 사용량 절약
     */
    private final Map<String, CachedKeywordData> memoryCache = new ConcurrentHashMap<>();

    /**
     * 기존 API 구조 유지 - 키워드 분석
     * 성능 최적화: 캐시 hit율 향상, 비동기 처리 추가
     */
    @Transactional
    public KeywordResponseDto analyzeKeywords(String accessToken, VisitedPagesRequest request) {
        User user = userService.findByAccessToken(accessToken);
        List<VisitedPageDto> visitedPages = request.getVisitedPages();

        validateVisitedPages(visitedPages);

        LocalDate today = LocalDate.now(KST_ZONE);
        String cacheKey = generateCacheKey(user.getId(), today);

        // 성능 최적화 1: 더 효율적인 캐시 체크
        KeywordResponseDto cachedResult = getCachedResult(user, today, cacheKey);
        if (cachedResult != null) {
            // 비동기로 캐시 갱신 체크 (백그라운드에서 실행)
            asyncCacheRefreshCheck(user, today, visitedPages.size());
            return cachedResult;
        }

        // 캐시 미스 시 OpenAI API 호출
        log.info("Cache miss - calling OpenAI API - userId: {}", user.getId());
        KeywordResponseDto result = callOpenAiApi(visitedPages);

        // 성능 최적화 2: 비동기 저장
        asyncSaveToAllCaches(cacheKey, user, result);

        return result;
    }

    /**
     * 기존 API 구조 유지 - 상위 키워드 조회
     * 성능 최적화: 더 효율적인 쿼리와 캐싱
     */
    public List<KeywordFrequencyDto> getTopKeywordsForToday(String accessToken) {
        User user = userService.findByAccessToken(accessToken);
        LocalDate today = LocalDate.now(KST_ZONE);

        // 성능 최적화 3: 더 스마트한 캐시 키
        String cacheKey = "top_keywords_" + user.getId() + "_" + today;

        // 메모리 캐시에서 먼저 확인
        CachedKeywordData cached = memoryCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.getTopKeywords();
        }

        List<KeywordData> todayKeywords = getTodayKeywordsFromDatabase(user, today);

        if (todayKeywords.isEmpty()) {
            return List.of();
        }

        // 성능 최적화 4: 스트림 연산 최적화
        List<KeywordFrequencyDto> topKeywords = todayKeywords.parallelStream()
                .collect(Collectors.groupingBy(
                        KeywordData::getKeyword,
                        Collectors.summingInt(KeywordData::getFrequency)))
                .entrySet().parallelStream()
                .map(entry -> new KeywordFrequencyDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()))
                .limit(9)
                .collect(Collectors.toList());

        // 결과를 메모리 캐시에 저장
        memoryCache.put(cacheKey, new CachedKeywordData(null, topKeywords));

        return topKeywords;
    }

    /**
     * 성능 최적화 1: 통합 캐시 체크
     */
    private KeywordResponseDto getCachedResult(User user, LocalDate today, String cacheKey) {
        // 메모리 캐시 우선 확인
        CachedKeywordData memoryCached = memoryCache.get(cacheKey);
        if (memoryCached != null && memoryCached.isValid()) {
            log.debug("Memory cache hit - userId: {}", user.getId());
            return memoryCached.getKeywordResponse();
        }

        // DB 캐시 확인
        Optional<KeywordResponseDto> dbCached = getKeywordsFromDatabase(user, today);
        if (dbCached.isPresent()) {
            log.debug("Database cache hit - userId: {}", user.getId());
            // 메모리 캐시에도 저장
            memoryCache.put(cacheKey, new CachedKeywordData(dbCached.get(), null));
            return dbCached.get();
        }

        return null;
    }

    /**
     * 성능 최적화 2: 비동기 캐시 갱신 체크
     * 사용자가 새로운 페이지를 많이 방문했으면 백그라운드에서 재분석
     */
    @Async
    public void asyncCacheRefreshCheck(User user, LocalDate date, int currentPageCount) {
        try {
            List<KeywordData> existingKeywords = getTodayKeywordsFromDatabase(user, date);
            int existingCount = existingKeywords.size();

            // 새로운 페이지가 50% 이상 증가했으면 캐시 무효화 고려
            if (currentPageCount > existingCount * 1.5) {
                log.info("Significant activity increase detected - userId: {}, existing: {}, current: {}",
                        user.getId(), existingCount, currentPageCount);
                // 필요시 캐시 무효화 로직 추가 가능
            }
        } catch (Exception e) {
            log.warn("Background cache refresh check failed", e);
        }
    }

    /**
     * 성능 최적화 3: 비동기 저장
     */
    @Async
    public void asyncSaveToAllCaches(String cacheKey, User user, KeywordResponseDto result) {
        try {
            // 메모리 캐시 저장
            memoryCache.put(cacheKey, new CachedKeywordData(result, null));

            // DB 저장
            saveToDatabase(user, result);

            log.debug("Async save completed - userId: {}", user.getId());
        } catch (Exception e) {
            log.error("Async save failed", e);
        }
    }

    /**
     * 성능 최적화 4: 배치 삭제로 메모리 관리
     */
    public void cleanupExpiredCache() {
        long now = System.currentTimeMillis();

        memoryCache.entrySet().removeIf(entry -> {
            return !entry.getValue().isValidAt(now);
        });

        log.debug("Cache cleanup completed - remaining entries: {}", memoryCache.size());
    }

    /**
     * DB에서 키워드 조회 (Spring Cache 적용)
     */
    @Cacheable(value = "dailyKeywords", key = "#user.id + '_' + #date")
    public Optional<KeywordResponseDto> getKeywordsFromDatabase(User user, LocalDate date) {
        List<KeywordData> todayKeywords = getTodayKeywordsFromDatabase(user, date);

        if (todayKeywords.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(convertToResponseDto(todayKeywords));
    }

    /**
     * 캐시 무효화
     */
    @CacheEvict(value = {"dailyKeywords", "topKeywords"}, key = "#user.id + '_' + #date")
    @Transactional
    public void invalidateCache(User user, LocalDate date) {
        String cacheKey = generateCacheKey(user.getId(), date);
        String topKeywordsCacheKey = "top_keywords_" + user.getId() + "_" + date;

        // 메모리 캐시 삭제
        memoryCache.remove(cacheKey);
        memoryCache.remove(topKeywordsCacheKey);

        // DB 데이터 삭제
        List<KeywordData> keywordsToDelete = getTodayKeywordsFromDatabase(user, date);
        if (!keywordsToDelete.isEmpty()) {
            keywordDataRepository.deleteAll(keywordsToDelete);
            log.info("Cache invalidated - userId: {}, date: {}", user.getId(), date);
        }
    }

    // 기존 메서드들 (성능 최적화 없이 유지)
    private void validateVisitedPages(List<VisitedPageDto> visitedPages) {
        if (visitedPages == null || visitedPages.isEmpty()) {
            throw new IllegalArgumentException("방문 페이지 데이터가 없습니다.");
        }
    }

    private KeywordResponseDto callOpenAiApi(List<VisitedPageDto> visitedPages) {
        try {
            String prompt = createPrompt(visitedPages);
            Map<String, Object> requestBody = createApiRequestBody(prompt);

            HttpEntity<Map<String, Object>> entity = createHttpEntity(requestBody);
            ResponseEntity<Map> response = openAiRestTemplate.postForEntity(openaiUri, entity, Map.class);

            return parseApiResponse(response);

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new RuntimeException("키워드 분석 실패: " + e.getMessage());
        }
    }

    private Map<String, Object> createApiRequestBody(String prompt) {
        return Map.of(
                "model", openaiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 인터넷 검색 기록을 보고 주요 키워드를 추출해주는 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );
    }

    private HttpEntity<Map<String, Object>> createHttpEntity(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return new HttpEntity<>(requestBody, headers);
    }

    private KeywordResponseDto parseApiResponse(ResponseEntity<Map> response) throws JsonProcessingException {
        List<?> choices = (List<?>) response.getBody().get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("OpenAI 응답에 선택지가 없습니다.");
        }

        Map<String, Object> choice = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        String content = Objects.toString(message.get("content"), "");

        return objectMapper.readValue(content, KeywordResponseDto.class);
    }

    private void saveToDatabase(User user, KeywordResponseDto dto) {
        if (dto.getKeywordFrequencies() == null || dto.getKeywordFrequencies().isEmpty()) {
            return;
        }

        List<KeywordData> keywordDataList = dto.getKeywordFrequencies().stream()
                .map(kf -> new KeywordData(user, kf.getKeyword(), kf.getFrequency()))
                .collect(Collectors.toList());

        keywordDataRepository.saveAll(keywordDataList);
        log.debug("Keywords saved - userId: {}, count: {}", user.getId(), keywordDataList.size());
    }

    private List<KeywordData> getTodayKeywordsFromDatabase(User user, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return keywordDataRepository.findByUserAndCreatedAtBetween(user, startOfDay, endOfDay);
    }

    private KeywordResponseDto convertToResponseDto(List<KeywordData> keywordDataList) {
        List<KeywordFrequencyDto> frequencies = keywordDataList.stream()
                .map(kd -> new KeywordFrequencyDto(kd.getKeyword(), kd.getFrequency()))
                .collect(Collectors.toList());

        KeywordResponseDto response = new KeywordResponseDto();
        response.setKeywordFrequencies(frequencies);
        return response;
    }

    private String generateCacheKey(Long userId, LocalDate date) {
        return userId + "_" + date.toString();
    }

    private String createPrompt(List<VisitedPageDto> visitedPages) throws JsonProcessingException {
        String template = """
                다음은 사용자의 웹 브라우징 기록입니다. 각 페이지 제목을 분석하여 핵심 키워드를 추출해주세요.
                
                **추출 규칙:**
                1. 각 제목당 가장 중요한 키워드 1개만 추출
                2. 한국어 키워드 우선 (예: "스프링부트", "리액트", "맛집")
                3. 기술 용어는 원어 그대로 (예: "JWT", "API", "Docker")
                4. 브랜드명/서비스명 포함 (예: "GitHub", "유튜브", "스타벅스")
                5. 일반적이고 의미없는 단어 제외 (예: "검색", "사이트", "페이지")
                
                **키워드 예시:**
                "GitHub - Spring Boot 프로젝트" → "GitHub"
                "유튜브 - React 강의 시청" → "React"
                "스타벅스 매장 찾기" → "스타벅스"
                "Stack Overflow - JWT 구현 질문" → "JWT"
                "배달의민족 치킨 주문" → "배달의민족"
                
                **출력 형식 (JSON만 출력):**
                {
                  "keywordFrequencies": [
                    { "keyword": "GitHub", "frequency": 5 },
                    { "keyword": "React", "frequency": 3 },
                    { "keyword": "스타벅스", "frequency": 2 }
                  ]
                }
                
                **분석할 데이터:**
                %s
                """;

        return String.format(template, objectMapper.writeValueAsString(Map.of("visitedPages", visitedPages)));
    }

    /**
     * 성능 최적화된 캐시 데이터 클래스
     */
    private static class CachedKeywordData {
        @Getter
        private final KeywordResponseDto keywordResponse;
        @Getter
        private final List<KeywordFrequencyDto> topKeywords;
        private final long cachedAt;

        public CachedKeywordData(KeywordResponseDto keywordResponse, List<KeywordFrequencyDto> topKeywords) {
            this.keywordResponse = keywordResponse;
            this.topKeywords = topKeywords;
            this.cachedAt = System.currentTimeMillis();
        }

        public boolean isValid() {
            return isValidAt(System.currentTimeMillis());
        }

        public boolean isValidAt(long currentTime) {
            return (currentTime - cachedAt) < CACHE_DURATION_MS;
        }
    }
}