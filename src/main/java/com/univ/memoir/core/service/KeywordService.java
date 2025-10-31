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
import com.univ.memoir.api.dto.req.page.VisitedPageDto;
import com.univ.memoir.api.dto.req.page.VisitedPagesRequest;
import com.univ.memoir.api.dto.res.KeywordFrequencyDto;
import com.univ.memoir.api.dto.res.keyword.KeywordResponseDto;
import com.univ.memoir.core.domain.KeywordData;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.KeywordDataRepository;
import com.univ.memoir.core.repository.UserRepository;
import com.univ.memoir.config.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 키워드 분석 서비스 - 성능 최적화 버전
 * N+1 문제 해결 + User 조회 최적화
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class KeywordService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final long CACHE_DURATION_MS = 6 * 60 * 60 * 1000L; // 6시간

    private final ObjectMapper objectMapper;
    private final RestTemplate openAiRestTemplate;
    private final UserService userService;
    private final KeywordDataRepository keywordDataRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Value("${openai.api.base-url}${openai.uri}")
    private String openaiUri;

    /**
     * 성능 최적화된 메모리 캐시
     */
    private final Map<String, CachedKeywordData> memoryCache = new ConcurrentHashMap<>();

    /**
     * 키워드 분석 - N+1 문제 해결
     * User는 딱 1번만 조회, 이후는 userId만 사용
     */
    @Transactional
    public KeywordResponseDto analyzeKeywords(String accessToken, VisitedPagesRequest request) {
        Long userId = extractUserIdFromToken(accessToken);

        List<VisitedPageDto> visitedPages = request.getVisitedPages();
        validateVisitedPages(visitedPages);

        LocalDate today = LocalDate.now(KST_ZONE);
        String cacheKey = generateCacheKey(userId, today);

        KeywordResponseDto cachedResult = getCachedResult(userId, today, cacheKey);
        if (cachedResult != null) {
            asyncCacheRefreshCheck(userId, today, visitedPages.size());
            return cachedResult;
        }

        log.info("Cache miss - calling OpenAI API - userId: {}", userId);
        KeywordResponseDto result = callOpenAiApi(visitedPages);

        asyncSaveToAllCaches(cacheKey, userId, result);

        return result;
    }

    /**
     * 상위 키워드 조회 - N+1 문제 해결
     */
    public List<KeywordFrequencyDto> getTopKeywordsForToday(String accessToken) {
        // ✅ userId 직접 추출 (User 조회 안 함!)
        Long userId = extractUserIdFromToken(accessToken);

        LocalDate today = LocalDate.now(KST_ZONE);
        String cacheKey = "top_keywords_" + userId + "_" + today;

        // 메모리 캐시 확인
        CachedKeywordData cached = memoryCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.getTopKeywords();
        }

        // ✅ userId 사용
        List<KeywordData> todayKeywords = getTodayKeywordsFromDatabase(userId, today);

        if (todayKeywords.isEmpty()) {
            return List.of();
        }

        // 스트림 연산 (parallelStream은 데이터 적을 때 오히려 느릴 수 있음)
        List<KeywordFrequencyDto> topKeywords = todayKeywords.stream()
                .collect(Collectors.groupingBy(
                        KeywordData::getKeyword,
                        Collectors.summingInt(KeywordData::getFrequency)))
                .entrySet().stream()
                .map(entry -> new KeywordFrequencyDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()))
                .limit(9)
                .collect(Collectors.toList());

        // 결과를 메모리 캐시에 저장
        memoryCache.put(cacheKey, new CachedKeywordData(null, topKeywords));

        return topKeywords;
    }

    /**
     * 통합 캐시 체크 - userId 사용
     */
    private KeywordResponseDto getCachedResult(Long userId, LocalDate today, String cacheKey) {
        // 메모리 캐시 우선 확인
        CachedKeywordData memoryCached = memoryCache.get(cacheKey);
        if (memoryCached != null && memoryCached.isValid()) {
            log.debug("Memory cache hit - userId: {}", userId);
            return memoryCached.getKeywordResponse();
        }

        // ✅ DB 캐시 확인 (userId 사용)
        Optional<KeywordResponseDto> dbCached = getKeywordsFromDatabase(userId, today);
        if (dbCached.isPresent()) {
            log.debug("Database cache hit - userId: {}", userId);
            memoryCache.put(cacheKey, new CachedKeywordData(dbCached.get(), null));
            return dbCached.get();
        }

        return null;
    }

    /**
     * 비동기 캐시 갱신 체크 - userId 사용
     */
    @Async
    public void asyncCacheRefreshCheck(Long userId, LocalDate date, int currentPageCount) {
        try {
            List<KeywordData> existingKeywords = getTodayKeywordsFromDatabase(userId, date);
            int existingCount = existingKeywords.size();

            if (currentPageCount > existingCount * 1.5) {
                log.info("Significant activity increase detected - userId: {}, existing: {}, current: {}",
                        userId, existingCount, currentPageCount);
            }
        } catch (Exception e) {
            log.warn("Background cache refresh check failed", e);
        }
    }

    /**
     * 비동기 저장 - userId 사용
     */
    @Async
    public void asyncSaveToAllCaches(String cacheKey, Long userId, KeywordResponseDto result) {
        try {
            // 메모리 캐시 저장
            memoryCache.put(cacheKey, new CachedKeywordData(result, null));

            // ✅ DB 저장 (User 필요할 때만 조회)
            User user = userService.findById(userId);
            saveToDatabase(user, result);

            log.debug("Async save completed - userId: {}", userId);
        } catch (Exception e) {
            log.error("Async save failed", e);
        }
    }

    /**
     * 배치 삭제로 메모리 관리
     */
    public void cleanupExpiredCache() {
        long now = System.currentTimeMillis();

        memoryCache.entrySet().removeIf(entry -> {
            return !entry.getValue().isValidAt(now);
        });

        log.debug("Cache cleanup completed - remaining entries: {}", memoryCache.size());
    }

    /**
     * DB에서 키워드 조회 - userId 사용
     */
    @Cacheable(value = "dailyKeywords", key = "#userId + '_' + #date")
    public Optional<KeywordResponseDto> getKeywordsFromDatabase(Long userId, LocalDate date) {
        List<KeywordData> todayKeywords = getTodayKeywordsFromDatabase(userId, date);

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
        Long userId = user.getId();
        String cacheKey = generateCacheKey(userId, date);
        String topKeywordsCacheKey = "top_keywords_" + userId + "_" + date;

        // 메모리 캐시 삭제
        memoryCache.remove(cacheKey);
        memoryCache.remove(topKeywordsCacheKey);

        // ✅ DB 데이터 삭제 (userId 사용)
        List<KeywordData> keywordsToDelete = getTodayKeywordsFromDatabase(userId, date);
        if (!keywordsToDelete.isEmpty()) {
            keywordDataRepository.deleteAll(keywordsToDelete);
            log.info("Cache invalidated - userId: {}, date: {}", userId, date);
        }
    }

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

        // Batch INSERT 활성화되어 있으면 자동으로 배치 처리됨
        keywordDataRepository.saveAll(keywordDataList);
        log.debug("Keywords saved - userId: {}, count: {}", user.getId(), keywordDataList.size());
    }

    /**
     * ✅ N+1 문제 해결: userId만 사용
     */
    private List<KeywordData> getTodayKeywordsFromDatabase(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return keywordDataRepository.findByUserIdAndCreatedAtBetween(
                userId,  // ✅ User 객체 대신 ID만!
                startOfDay,
                endOfDay
        );
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

    /**
     * ✅ 토큰에서 userId 직접 추출 (User 조회 안 함!)
     */
    private Long extractUserIdFromToken(String accessToken) {
        // Bearer 접두사 제거
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7).trim();
        }

        String email = jwtProvider.getEmailFromToken(accessToken);

        // ✅ ID만 조회! (interests/bookmarks 안 가져옴)
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
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
                6. 의미 있고 이해 가능한 단어만 추출 (기술적 코드나 무의미한 문자열 제외)
                
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