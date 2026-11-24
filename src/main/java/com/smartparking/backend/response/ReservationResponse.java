
package com.smartparking.backend.response;

import com.smartparking.backend.entity.Reservation;

import java.time.LocalDateTime;

public record ReservationResponse(

        Long id,
        String reservationCode,
        String customerName,
        String phone,
        String licensePlate,
        String floor,
        String spot,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime createdAt

) {

    public static ReservationResponse from(Reservation source) {

        return new ReservationResponse(
                source.getId(),
                source.getReservationCode(),
                source.getCustomerName(),
                source.getPhone(),
                source.getLicensePlate(),
                source.getSpot().getFloorCode(),
                source.getSpot().getSpotCode(),
                source.getStartTime(),
                source.getEndTime(),
                source.getStatus(),
                source.getCreatedAt()
        );
    }
}
