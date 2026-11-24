package com.smartparking.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QrScanRequest(

        @NotBlank(message = "QR Payload không được để trống")
        @Size(max = 255, message = "QR Payload quá dài")
        String qrPayload

) {
}
