
package com.smartparking.backend.request;

import com.smartparking.backend.enums.ParkingSessionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParkingSessionCheckInRequest(

        @NotBlank(message = "QR Payload không được để trống")
        @Size(max = 255)
        String qrPayload,

        // Trường tùy chọn để tương thích luồng cũ.
        // Backend không sử dụng giá trị này để phân loại khách.
        ParkingSessionType sessionType,

        @Size(max = 255)
        String reservationCode,

        @Size(max = 1)
        String userCode,

        @Size(max = 100)
        String customerName,

        @Size(max = 20)
        String phone,

        @Size(max = 20)
        String licensePlate,

        boolean overnightRequested

) {
}
