
package com.smartparking.backend.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulationClockSpeedRequest(

        @NotNull(message = "Tốc độ không được để trống")

        @DecimalMin(
                value = "0.01",
                message = "Tốc độ phải lớn hơn hoặc bằng 0.01"
        )

        @Digits(
                integer = 6,
                fraction = 2,
                message = "Tốc độ tối đa 6 chữ số nguyên và 2 chữ số thập phân"
        )

        BigDecimal speedMultiplier

) {
}
