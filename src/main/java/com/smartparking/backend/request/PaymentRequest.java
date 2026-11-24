
package com.smartparking.backend.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(

        @NotBlank(message = "Mã phiên gửi xe không được để trống")
        String sessionCode

) {
}
