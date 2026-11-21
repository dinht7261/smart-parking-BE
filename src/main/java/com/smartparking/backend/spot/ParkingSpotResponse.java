package com.smartparking.backend.spot;

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
