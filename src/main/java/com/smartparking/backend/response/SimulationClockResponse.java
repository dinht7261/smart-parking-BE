
package com.smartparking.backend.response;

import com.smartparking.backend.entity.SimulationClock;
import com.smartparking.backend.enums.ClockType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record SimulationClockResponse(

        ClockType clockType,

        LocalDateTime initialTime,

        LocalDateTime simulatedTime,

        boolean running,

        BigDecimal speedMultiplier,

        OffsetDateTime lastRealTime,

        OffsetDateTime updatedAt

) {

    public static SimulationClockResponse from(
            SimulationClock clock
    ) {

        return new SimulationClockResponse(
                clock.getClockType(),
                clock.getInitialTime(),
                clock.getSimulatedTime(),
                clock.isRunning(),
                clock.getSpeedMultiplier(),
                clock.getLastRealTime(),
                clock.getUpdatedAt()
        );
    }
}
