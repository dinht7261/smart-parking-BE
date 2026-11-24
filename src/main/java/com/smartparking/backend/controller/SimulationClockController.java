
package com.smartparking.backend.controller;

import com.smartparking.backend.enums.ClockType;
import com.smartparking.backend.request.SimulationClockTimeRequest;
import com.smartparking.backend.request.SimulationClockSpeedRequest;
import com.smartparking.backend.response.SimulationClockResponse;
import com.smartparking.backend.service.SimulationClockService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulation-clocks")
public class SimulationClockController {

    private final SimulationClockService service;

    public SimulationClockController(
            SimulationClockService service
    ) {
        this.service = service;
    }

    // =====================================
    // 1. LẤY TOÀN BỘ ĐỒNG HỒ
    // =====================================

    @GetMapping
    public ResponseEntity<List<SimulationClockResponse>> getAll() {

        List<SimulationClockResponse> response =
                service.getAll()
                        .stream()
                        .map(SimulationClockResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =====================================
    // 2. LẤY ĐỒNG HỒ THEO LOẠI
    // =====================================

    @GetMapping("/{type}")
    public ResponseEntity<SimulationClockResponse> getByType(
            @PathVariable ClockType type
    ) {

        return ResponseEntity.ok(
                SimulationClockResponse.from(
                        service.getByType(type)
                )
        );
    }

    // =====================================
    // 3. START CLOCK
    // =====================================

    @PostMapping("/{type}/start")
    public ResponseEntity<SimulationClockResponse> start(
            @PathVariable ClockType type
    ) {

        return ResponseEntity.ok(
                SimulationClockResponse.from(
                        service.start(type)
                )
        );
    }

    // =====================================
    // 4. PAUSE CLOCK
    // =====================================

    @PostMapping("/{type}/pause")
    public ResponseEntity<SimulationClockResponse> pause(
            @PathVariable ClockType type
    ) {

        return ResponseEntity.ok(
                SimulationClockResponse.from(
                        service.pause(type)
                )
        );
    }

    // =====================================
    // 5. RESET CLOCK
    // =====================================

    @PostMapping("/{type}/reset")
    public ResponseEntity<SimulationClockResponse> reset(
            @PathVariable ClockType type
    ) {

        return ResponseEntity.ok(
                SimulationClockResponse.from(
                        service.reset(type)
                )
        );
    }

    // =====================================
    // 6. ĐIỀU CHỈNH THỜI GIAN
    // =====================================

    @PutMapping("/{type}/time")
    public ResponseEntity<SimulationClockResponse> setTime(
            @PathVariable ClockType type,
            @Valid @RequestBody SimulationClockTimeRequest request
    ) {

        return ResponseEntity.ok(
                SimulationClockResponse.from(
                        service.setTime(
                                type,
                                request.simulatedTime()
                        )
                )
        );
    }

    // =====================================
    // 7. ĐIỀU CHỈNH TỐC ĐỘ
    // =====================================

    @PutMapping("/{type}/speed")
    public ResponseEntity<SimulationClockResponse> setSpeed(
            @PathVariable ClockType type,
            @Valid @RequestBody SimulationClockSpeedRequest request
    ) {

        return ResponseEntity.ok(
                SimulationClockResponse.from(
                        service.setSpeed(
                                type,
                                request.speedMultiplier()
                        )
                )
        );
    }
}
