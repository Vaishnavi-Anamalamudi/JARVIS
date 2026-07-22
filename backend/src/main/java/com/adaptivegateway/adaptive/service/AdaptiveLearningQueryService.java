package com.adaptivegateway.adaptive.service;

import com.adaptivegateway.adaptive.dto.RateLimitAdjustmentResponse;
import com.adaptivegateway.adaptive.dto.RouteMetricResponse;
import com.adaptivegateway.adaptive.dto.TrafficHeatmapBucketResponse;
import com.adaptivegateway.adaptive.mapper.AdaptiveLearningMapper;
import com.adaptivegateway.adaptive.repository.RateLimitAdjustmentRepository;
import com.adaptivegateway.adaptive.repository.RouteMetricRepository;
import com.adaptivegateway.adaptive.repository.TrafficHeatmapBucketRepository;
import com.adaptivegateway.common.pagination.PageResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdaptiveLearningQueryService {

    private static final List<String> METRIC_SORTS = List.of("calculatedAt", "requestsPerSecond", "blockedRate", "errorRate");
    private static final List<String> HEATMAP_SORTS = List.of("bucketStart", "requestCount", "blockedCount", "hourOfDay");
    private static final List<String> ADJUSTMENT_SORTS = List.of("createdAt", "previousStrictnessFactor", "newStrictnessFactor");

    private final RouteMetricRepository routeMetricRepository;
    private final TrafficHeatmapBucketRepository heatmapRepository;
    private final RateLimitAdjustmentRepository adjustmentRepository;
    private final AdaptiveLearningMapper mapper;

    public AdaptiveLearningQueryService(
            RouteMetricRepository routeMetricRepository,
            TrafficHeatmapBucketRepository heatmapRepository,
            RateLimitAdjustmentRepository adjustmentRepository,
            AdaptiveLearningMapper mapper
    ) {
        this.routeMetricRepository = routeMetricRepository;
        this.heatmapRepository = heatmapRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteMetricResponse> listRouteMetrics(int page, int size, String sort, String direction) {
        return PageResponse.from(routeMetricRepository
                .findByDeletedAtIsNull(pageRequest(page, size, sort, direction, METRIC_SORTS, "calculatedAt"))
                .map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<TrafficHeatmapBucketResponse> listHeatmapBuckets(int page, int size, String sort, String direction) {
        return PageResponse.from(heatmapRepository
                .findByDeletedAtIsNull(pageRequest(page, size, sort, direction, HEATMAP_SORTS, "bucketStart"))
                .map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<RateLimitAdjustmentResponse> listAdjustments(int page, int size, String sort, String direction) {
        return PageResponse.from(adjustmentRepository
                .findByDeletedAtIsNull(pageRequest(page, size, sort, direction, ADJUSTMENT_SORTS, "createdAt"))
                .map(mapper::toResponse));
    }

    private PageRequest pageRequest(
            int page,
            int size,
            String sort,
            String direction,
            List<String> allowedSorts,
            String defaultSort
    ) {
        String sortProperty = allowedSorts.contains(sort) ? sort : defaultSort;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}
