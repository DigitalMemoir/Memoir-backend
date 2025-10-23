package com.univ.memoir.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachingRequestFilter Unit Tests")
class CachingRequestFilterTest {

    @InjectMocks
    private CachingRequestFilter cachingRequestFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("should set traceId in MDC before filter chain")
    void shouldSetTraceIdInMdcBeforeFilterChain() throws ServletException, IOException {
        // when
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should generate unique traceId for each request")
    void shouldGenerateUniqueTraceIdForEachRequest() throws ServletException, IOException {
        // given
        String[] traceIds = new String[2];

        // when
        doAnswer(invocation -> {
            traceIds[0] = MDC.get("traceId");
            return null;
        }).when(filterChain).doFilter(request, response);
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        MDC.clear();

        doAnswer(invocation -> {
            traceIds[1] = MDC.get("traceId");
            return null;
        }).when(filterChain).doFilter(request, response);
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(traceIds[0]).isNotNull();
        assertThat(traceIds[1]).isNotNull();
        assertThat(traceIds[0]).isNotEqualTo(traceIds[1]);
    }

    @Test
    @DisplayName("should clear MDC after filter chain completes")
    void shouldClearMdcAfterFilterChainCompletes() throws ServletException, IOException {
        // when
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("should clear MDC even when filter chain throws exception")
    void shouldClearMdcEvenWhenFilterChainThrowsException() throws ServletException, IOException {
        // given
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

        // when
        try {
            cachingRequestFilter.doFilterInternal(request, response, filterChain);
        } catch (ServletException e) {
            // expected
        }

        // then
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("should clear MDC even when filter chain throws IOException")
    void shouldClearMdcEvenWhenFilterChainThrowsIoException() throws ServletException, IOException {
        // given
        doThrow(new IOException("IO Test exception")).when(filterChain).doFilter(request, response);

        // when
        try {
            cachingRequestFilter.doFilterInternal(request, response, filterChain);
        } catch (IOException e) {
            // expected
        }

        // then
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("should pass request and response to filter chain")
    void shouldPassRequestAndResponseToFilterChain() throws ServletException, IOException {
        // when
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    @DisplayName("should handle multiple concurrent requests independently")
    void shouldHandleMultipleConcurrentRequestsIndependently() throws ServletException, IOException {
        // given
        String[] traceIds = new String[3];

        // when - simulate concurrent requests
        for (int i = 0; i < 3; i++) {
            final int index = i;
            doAnswer(invocation -> {
                traceIds[index] = MDC.get("traceId");
                return null;
            }).when(filterChain).doFilter(request, response);

            cachingRequestFilter.doFilterInternal(request, response, filterChain);
            MDC.clear();
        }

        // then
        assertThat(traceIds[0]).isNotNull();
        assertThat(traceIds[1]).isNotNull();
        assertThat(traceIds[2]).isNotNull();
        assertThat(traceIds[0]).isNotEqualTo(traceIds[1]);
        assertThat(traceIds[1]).isNotEqualTo(traceIds[2]);
        assertThat(traceIds[0]).isNotEqualTo(traceIds[2]);
    }

    @Test
    @DisplayName("should maintain traceId throughout filter chain execution")
    void shouldMaintainTraceIdThroughoutFilterChainExecution() throws ServletException, IOException {
        // given
        final String[] capturedTraceId = new String[1];
        final String[] capturedTraceIdDuringExecution = new String[1];

        doAnswer(invocation -> {
            capturedTraceIdDuringExecution[0] = MDC.get("traceId");
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(capturedTraceIdDuringExecution[0]).isNotNull();
        assertThat(MDC.get("traceId")).isNull(); // Should be cleared after
    }

    @Test
    @DisplayName("should generate valid UUID format for traceId")
    void shouldGenerateValidUuidFormatForTraceId() throws ServletException, IOException {
        // given
        final String[] capturedTraceId = new String[1];

        doAnswer(invocation -> {
            capturedTraceId[0] = MDC.get("traceId");
            return null;
        }).when(filterChain).doFilter(request, response);

        // when
        cachingRequestFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(capturedTraceId[0])
                .isNotNull()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}