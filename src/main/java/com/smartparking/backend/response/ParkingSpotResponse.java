package com.smartparking.backend.response;

import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.enums.ParkingSpotStatus;

public record ParkingSpotResponse(
    Long id,
    String floor,
    String spot,
    ParkingSpotStatus status
) {
    public static ParkingSpotResponse from(ParkingSpot source) {
        return new ParkingSpotResponse(
            source.getId(), source.getFloorCode(), source.getSpotCode(), source.getStatus()
        );
    }
}
