
package com.smartparking.backend.controller;

import com.smartparking.backend.response.PaymentResponse;
import com.smartparking.backend.service.PaymentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }


    // =====================================
    // 1. TẠO HÓA ĐƠN THANH TOÁN
    // =====================================

    @PostMapping("/sessions/{sessionCode}")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable String sessionCode
    ) {

        PaymentResponse response =
                service.createPaymentResponse(sessionCode);

        return ResponseEntity.ok(response);
    }


    // =====================================
    // 2. XEM CHI TIẾT HÓA ĐƠN
    // =====================================

    @GetMapping("/{paymentCode}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String paymentCode
    ) {

        PaymentResponse response =
                service.getPaymentResponse(paymentCode);

        return ResponseEntity.ok(response);
    }


    // =====================================
    // 3. XÁC NHẬN THANH TOÁN MÔ PHỎNG
    // =====================================

    @PostMapping("/{paymentCode}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable String paymentCode
    ) {

        PaymentResponse response =
                service.confirmPaymentResponse(paymentCode);

        return ResponseEntity.ok(response);
    }

    // =====================================
// 4. LẤY LỊCH SỬ THANH TOÁN
// =====================================

    @GetMapping
    public ResponseEntity<List<PaymentResponse>>
    getPaymentHistory() {

        return ResponseEntity.ok(
                service.getPaymentHistory()
        );
    }

}
