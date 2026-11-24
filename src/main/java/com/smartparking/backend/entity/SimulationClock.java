
package com.smartparking.backend.entity;

import com.smartparking.backend.enums.ClockType;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "simulation_clocks", schema = "public")
public class SimulationClock {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "clock_type", nullable = false, length = 3)
    private ClockType clockType;

    @Column(name = "initial_time", nullable = false)
    private LocalDateTime initialTime;

    @Column(name = "simulated_time", nullable = false)
    private LocalDateTime simulatedTime;

    @Column(name = "is_running", nullable = false)
    private boolean running;

    @Column(name = "speed_multiplier",
            nullable = false,
            precision = 8,
            scale = 2)
    private BigDecimal speedMultiplier;

    @Column(name = "last_real_time")
    private OffsetDateTime lastRealTime;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SimulationClock() {
    }

    // Getters

    public ClockType getClockType() {
        return clockType;
    }

    public LocalDateTime getInitialTime() {
        return initialTime;
    }

    public LocalDateTime getSimulatedTime() {
        return simulatedTime;
    }

    public boolean isRunning() {
        return running;
    }

    public BigDecimal getSpeedMultiplier() {
        return speedMultiplier;
    }

    public OffsetDateTime getLastRealTime() {
        return lastRealTime;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Bắt đầu đồng hồ

    public void start(OffsetDateTime realNow) {
        if (!running) {
            this.running = true;
            this.lastRealTime = realNow;
        }
    }

    // Tạm dừng đồng hồ

    public void pause(OffsetDateTime realNow) {
        this.running = false;
        this.lastRealTime = realNow;
    }

    // Reset về thời gian ban đầu

    public void reset(OffsetDateTime realNow) {
        this.simulatedTime = this.initialTime;
        this.running = false;
        this.lastRealTime = realNow;
    }

    // Cập nhật thời gian mô phỏng

    public void updateSimulatedTime(
            LocalDateTime simulatedTime,
            OffsetDateTime realNow
    ) {
        this.simulatedTime = simulatedTime;
        this.lastRealTime = realNow;
    }

    // Thay đổi tốc độ đồng hồ

    public void updateSpeedMultiplier(
            BigDecimal speedMultiplier,
            OffsetDateTime realNow
    ) {
        this.speedMultiplier = speedMultiplier;
        this.lastRealTime = realNow;
    }

    // Tự động cập nhật thời gian sửa đổi

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        this.updatedAt = OffsetDateTime.now();
    }
}
