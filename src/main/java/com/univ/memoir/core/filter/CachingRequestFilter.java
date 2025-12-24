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
 * 요청 기반 로깅 및 traceId 관리 필터
 */
@Slf4j
@Component
public class CachingRequestFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // NGINX에서 전달한 X-Request-Id 헤더를 우선 사용, 없으면 UUID 생성
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID, traceId);
        response.setHeader(REQUEST_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}