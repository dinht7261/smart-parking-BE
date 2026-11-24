
package com.smartparking.backend.service;

import com.smartparking.backend.response.QrScanResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QrService {

    // Chỉ sử dụng QR cổng IN
    @Value("${parking.qr.in-payload}")
    private String inPayload;

    // =====================================
    // XỬ LÝ QUÉT QR
    // =====================================

    public QrScanResponse scan(String qrPayload) {

        if (qrPayload == null || qrPayload.isBlank()) {

            return new QrScanResponse(
                    false,
                    null,
                    null,
                    "QR không được để trống"
            );
        }

        String payload = qrPayload.trim();

        // Chỉ chấp nhận QR cổng IN
        if (!payload.equals(inPayload)) {

            return new QrScanResponse(
                    false,
                    null,
                    null,
                    "QR không hợp lệ hoặc không thuộc hệ thống"
            );
        }

        // Phân tích thông tin QR
        String[] parts = payload.split(":");

        if (parts.length != 3 || !"IN".equals(parts[1])) {

            return new QrScanResponse(
                    false,
                    null,
                    null,
                    "QR cổng vào không đúng định dạng"
            );
        }

        // QR hợp lệ
        return new QrScanResponse(
                true,
                parts[2],
                "IN",
                "QR cổng vào hợp lệ"
        );
    }
}
