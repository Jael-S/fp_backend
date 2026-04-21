package com.flowpolicy.common.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    long totalItems,
    int page,
    int size
) {}

