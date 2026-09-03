package com.adaptivegateway.analytics.dto;

import com.adaptivegateway.analytics.enums.AnalyticsGranularity;
import java.time.Instant;

public record AnalyticsRunResponse(
        AnalyticsGranularity granularity,
        int bucketsEvaluated,
        int rollupsWritten,
        Instant startedAt,
        Instant completedAt
) {
}
