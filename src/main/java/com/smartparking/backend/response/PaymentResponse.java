
package com.smartparking.backend.response;

import com.smartparking.backend.entity.Payment;
import com.smartparking.backend.entity.ParkingSession;

import com.smartparking.backend.enums.PaymentMethod;
import com.smartparking.backend.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(

        // Thông tin giao dịch
        Long id,
        String paymentCode,

        // Thông tin phiên gửi xe
        String sessionCode,
        String licensePlate,

        // Thời gian gửi xe
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,

        // Đăng ký qua đêm
        boolean overnightRequested,

// Chi tiết hóa đơn
        int billableHours,
        BigDecimal parkingFee,

// Phụ phí giữ chỗ do Check-in muộn
        int reservationHoldHours,
        BigDecimal reservationHoldFee,

// Phạt qua đêm
        int violationNights,
        BigDecimal overnightPenalty,

        // Tổng tiền
        BigDecimal amount,

        // Thông tin thanh toán
        PaymentMethod paymentMethod,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime createdAt

) {

    // =====================================
    // CHUYỂN PAYMENT ENTITY THÀNH RESPONSE
    // =====================================

    public static PaymentResponse from(Payment payment) {

        ParkingSession session = payment.getParkingSession();

        return new PaymentResponse(

                payment.getId(),
                payment.getPaymentCode(),

                session.getSessionCode(),
                session.getLicensePlate(),

                session.getCheckInTime(),
                session.getCheckOutTime(),

                session.isOvernightRequested(),

                payment.getBillableHours(),
                payment.getParkingFee(),

                payment.getReservationHoldHours(),
                payment.getReservationHoldFee(),

                payment.getViolationNights(),
                payment.getOvernightPenalty(),

                payment.getAmount(),

                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
