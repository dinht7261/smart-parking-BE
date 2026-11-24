
package com.smartparking.backend.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SimulationClockTimeRequest(

        @NotNull(message = "Thời gian mô phỏng không được để trống")
        LocalDateTime simulatedTime

) {
}
