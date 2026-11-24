
package com.smartparking.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record OvernightRequestRequest(

        @NotBlank(message = "User code không được để trống")
        @Size(max = 1)
        String userCode,

        @NotBlank(message = "Session code không được để trống")
        @Size(max = 30)
        String sessionCode,

        @NotNull(message = "Vui lòng chọn ngày qua đêm")
        LocalDate nightDate,

        @NotBlank(message = "Vui lòng nhập lý do")
        @Size(max = 500)
        String reason

) {
}
