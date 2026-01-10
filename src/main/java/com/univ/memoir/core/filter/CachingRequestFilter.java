package com.univ.memoir.core.filter;

import com.univ.memoir.core.util.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 요청 기반 로깅 및 추적 ID 관리 필터
 */
@Slf4j
@Component
public class CachingRequestFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // traceId: 애플리케이션 내부 분산 추적용 (항상 생성)
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);

        // requestId: NGINX에서 전달받은 요청 ID (prod 환경)
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.trim().isEmpty()) {
            MDC.put(REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            log.debug("NGINX requestId: {}, traceId: {}", requestId, traceId);
        } else {
            log.debug("Generated traceId: {}", traceId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST_ID);
        }
    }
}