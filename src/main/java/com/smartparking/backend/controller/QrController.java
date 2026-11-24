
package com.smartparking.backend.controller;

import com.smartparking.backend.request.QrScanRequest;
import com.smartparking.backend.response.QrScanResponse;
import com.smartparking.backend.service.QrService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@CrossOrigin(origins = "http://localhost:4200")
public class QrController {

    private final QrService service;

    public QrController(QrService service) {
        this.service = service;
    }

    // =====================================
    // QUÉT VÀ XÁC THỰC QR
    // =====================================

    @PostMapping("/scan")
    public ResponseEntity<QrScanResponse> scanQr(
            @Valid @RequestBody QrScanRequest request
    ) {

        QrScanResponse response =
                service.scan(request.qrPayload());

        return ResponseEntity.ok(response);
    }

}
