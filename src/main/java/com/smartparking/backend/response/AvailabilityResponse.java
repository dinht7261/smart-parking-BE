package com.smartparking.backend.response;

public record AvailabilityResponse(
    long total,
    long available,
    long reserved,
    long occupied,
    long maintenance
) {}
