package com.enterprise.ticketing.common.util;

import org.slf4j.MDC;

public final class TraceIdUtils {

    private TraceIdUtils() {
    }

    public static String currentTraceId() {
        return MDC.get("traceId");
    }
}

