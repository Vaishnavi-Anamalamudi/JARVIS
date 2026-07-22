package com.adaptivegateway.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequestParams(
        @Min(0) int page,
        @Min(1) @Max(100) int size,
        String sort,
        String direction
) {

    public PageRequestParams {
        sort = sort == null || sort.isBlank() ? "createdAt" : sort;
        direction = direction == null || direction.isBlank() ? "desc" : direction.toLowerCase();
    }
}
