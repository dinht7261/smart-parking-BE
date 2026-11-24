
package com.smartparking.backend.response;

import com.smartparking.backend.entity.ParkingSession;
import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.entity.Reservation;

import com.smartparking.backend.enums.ParkingSessionStatus;
import com.smartparking.backend.enums.ParkingSessionType;

import java.time.LocalDateTime;

public record ParkingSessionResponse(

        Long id,

        String sessionCode,

        String exitQrPayload,

        ParkingSessionType sessionType,

        String customerName,

        String phone,

        String licensePlate,

        Long spotId,

        String floorCode,

        String spotCode,

        String reservationCode,

        LocalDateTime checkInTime,

        LocalDateTime checkOutTime,

        ParkingSessionStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {

    public static ParkingSessionResponse from(
            ParkingSession session
    ) {

        ParkingSpot spot = session.getSpot();

        Reservation reservation = session.getReservation();

        return new ParkingSessionResponse(

                session.getId(),

                session.getSessionCode(),

                "SMARTPARKING:SESSION:" +
                        session.getExitQrToken(),

                session.getSessionType(),

                session.getCustomerName(),

                session.getPhone(),

                session.getLicensePlate(),

                spot != null ? spot.getId() : null,

                spot != null ? spot.getFloorCode() : null,

                spot != null ? spot.getSpotCode() : null,

                reservation != null
                        ? reservation.getReservationCode()
                        : null,

                session.getCheckInTime(),

                session.getCheckOutTime(),

                session.getStatus(),

                session.getCreatedAt(),

                session.getUpdatedAt()
        );
    }
}
