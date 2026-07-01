package com.securetask.backend.audit;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_ATTRIBUTE =
            CorrelationIdFilter.class.getName() + ".correlationId";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private static final Pattern SAFE_CORRELATION_ID =
            Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || !SAFE_CORRELATION_ID.matcher(correlationId).matches()) {
            correlationId = UUID.randomUUID().toString();
        }

        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        // MDC is thread-bound, and servlet container threads are pooled and
        // reused across unrelated requests - always clear it, even on error,
        // or it leaks into the next request logged on this thread.
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
