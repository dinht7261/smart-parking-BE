
package com.smartparking.backend.response;

public record ParkingAvailabilityResponse(

        long totalSpots,

        long availableSpots,

        long reservedSpots,

        long occupiedSpots,

        long maintenanceSpots,

        String displayText

) {
}
