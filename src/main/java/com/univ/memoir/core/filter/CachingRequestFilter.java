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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();

        MDC.put(TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}