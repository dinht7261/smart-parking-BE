
package com.smartparking.backend.controller;

import com.smartparking.backend.request.OvernightRequestRequest;
import com.smartparking.backend.response.OvernightRequestResponse;
import com.smartparking.backend.service.OvernightRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1/overnight-requests")
public class OvernightRequestController {

    private final OvernightRequestService service;

    public OvernightRequestController(
            OvernightRequestService service
    ) {
        this.service = service;
    }


    // =====================================
    // 1. TẠO YÊU CẦU QUA ĐÊM
    // =====================================

    @PostMapping
    public ResponseEntity<OvernightRequestResponse> create(
            @Valid @RequestBody OvernightRequestRequest request
    ) {

        OvernightRequestResponse response =
                service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================
    // 2. LẤY YÊU CẦU CỦA USER
    // =====================================

    @GetMapping("/user/{userCode}")
    public ResponseEntity<List<OvernightRequestResponse>>
    listByUser(
            @PathVariable String userCode
    ) {

        return ResponseEntity.ok(
                service.listByUserCode(userCode)
        );
    }


    // =====================================
    // 3. OPERATOR LẤY TOÀN BỘ YÊU CẦU
    // =====================================

    @GetMapping
    public ResponseEntity<List<OvernightRequestResponse>>
    listAll() {

        return ResponseEntity.ok(
                service.listAll()
        );
    }


    // =====================================
    // 4. HỦY YÊU CẦU
    // =====================================

    @PatchMapping("/{requestCode}/cancel")
    public ResponseEntity<OvernightRequestResponse> cancel(
            @PathVariable String requestCode,
            @RequestParam String userCode
    ) {

        return ResponseEntity.ok(
                service.cancel(requestCode, userCode)
        );
    }

}
