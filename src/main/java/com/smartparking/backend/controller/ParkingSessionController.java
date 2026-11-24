
package com.smartparking.backend.controller;

import com.smartparking.backend.request.ParkingSessionCheckInRequest;
import com.smartparking.backend.response.ParkingSessionResponse;
import com.smartparking.backend.service.ParkingSessionService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionService service;

    public ParkingSessionController(
            ParkingSessionService service
    ) {
        this.service = service;
    }


    // =====================================
    // 1. CHECK-IN
    // =====================================

    @PostMapping("/check-in")
    public ResponseEntity<ParkingSessionResponse> checkIn(
            @Valid @RequestBody ParkingSessionCheckInRequest request
    ) {

        ParkingSessionResponse response =
                service.checkIn(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================
    // 2. CHECK-OUT
    // =====================================

    @PostMapping("/{sessionCode}/check-out")
    public ResponseEntity<ParkingSessionResponse> checkOut(
            @PathVariable String sessionCode
    ) {

        return ResponseEntity.ok(
                service.checkOut(sessionCode)
        );
    }


    // =====================================
    // 3. LẤY DANH SÁCH XE ĐANG GỬI
    // =====================================

    @GetMapping("/active")
    public ResponseEntity<List<ParkingSessionResponse>>
    getActiveSessions() {

        return ResponseEntity.ok(
                service.getActiveSessions()
        );
    }


    // =====================================
    // 4. LẤY LỊCH SỬ GỬI XE
    // =====================================

    @GetMapping("/history")
    public ResponseEntity<List<ParkingSessionResponse>>
    getSessionHistory() {

        return ResponseEntity.ok(
                service.getSessionHistory()
        );
    }


    // =====================================
    // 5. TRA CỨU PHIÊN GỬI XE
    // =====================================

    @GetMapping("/{sessionCode}")
    public ResponseEntity<ParkingSessionResponse>
    getSessionByCode(
            @PathVariable String sessionCode
    ) {

        return ResponseEntity.ok(
                service.getSessionByCode(sessionCode)
        );
    }
}
