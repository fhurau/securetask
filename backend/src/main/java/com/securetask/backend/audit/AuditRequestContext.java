package com.securetask.backend.audit;

import jakarta.servlet.http.HttpServletRequest;

public record AuditRequestContext(
        String ipAddress,
        String userAgent,
        String correlationId) {

    public static AuditRequestContext from(HttpServletRequest request) {
        return new AuditRequestContext(
                truncate(request.getRemoteAddr(), 45),
                truncate(request.getHeader("User-Agent"), 500),
                (String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
