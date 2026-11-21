package com.smartparking.backend.spot;

public record AvailabilityResponse(
    long total,
    long available,
    long reserved,
    long occupied,
    long maintenance
) {}
