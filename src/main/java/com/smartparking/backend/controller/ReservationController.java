
package com.smartparking.backend.controller;

import com.smartparking.backend.request.ReservationRequest;
import com.smartparking.backend.response.ReservationResponse;
import com.smartparking.backend.service.ReservationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    // =====================================
    // 1. TẠO ĐẶT CHỖ MỚI
    // =====================================

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request
    ) {

        ReservationResponse response = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================
    // 2. LẤY LỊCH SỬ ĐẶT CHỖ CỦA USER
    // =====================================

    @GetMapping("/user/{userCode}")
    public List<ReservationResponse> listByUser(
            @PathVariable String userCode
    ) {

        return service.listByUserCode(userCode);
    }

    // =====================================
    // 3. LẤY TOÀN BỘ DANH SÁCH ĐẶT CHỖ
    // =====================================

    @GetMapping
    public List<ReservationResponse> listAll() {

        return service.listAll();
    }

    // =====================================
    // 4. HỦY ĐẶT CHỖ
    // =====================================

    @PatchMapping("/{reservationCode}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable String reservationCode,
            @RequestParam String userCode
    ) {

        ReservationResponse response = service.cancel(
                reservationCode,
                userCode
        );

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{reservationCode}/confirm")
    public ResponseEntity<ReservationResponse> confirm(
            @PathVariable String reservationCode
    ) {

        return ResponseEntity.ok(
                service.confirm(reservationCode)
        );
    }


    @PatchMapping("/{reservationCode}/reject")
    public ResponseEntity<ReservationResponse> reject(
            @PathVariable String reservationCode
    ) {

        return ResponseEntity.ok(
                service.reject(reservationCode)
        );
    }


}
