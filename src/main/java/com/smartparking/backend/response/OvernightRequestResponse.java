
package com.smartparking.backend.response;

import com.smartparking.backend.entity.OvernightRequest;
import com.smartparking.backend.enums.OvernightRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OvernightRequestResponse(

        Long id,

        String requestCode,

        String sessionCode,

        String userCode,

        String licensePlate,

        LocalDate nightDate,

        String reason,

        OvernightRequestStatus status,

        LocalDateTime createdAt,

        LocalDateTime approvedAt,

        LocalDateTime cancelledAt

) {

    public static OvernightRequestResponse from(
            OvernightRequest request
    ) {

        return new OvernightRequestResponse(

                request.getId(),

                request.getRequestCode(),

                request.getParkingSession().getSessionCode(),

                request.getUser().getUserCode(),

                request.getLicensePlate(),

                request.getNightDate(),

                request.getReason(),

                request.getStatus(),

                request.getCreatedAt(),

                request.getApprovedAt(),

                request.getCancelledAt()

        );
    }
}
