
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.Payment;
import java.util.List;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    // =====================================
    // 1. TÌM GIAO DỊCH THEO MÃ THANH TOÁN
    // =====================================

    Optional<Payment> findByPaymentCode(
            String paymentCode
    );


    // =====================================
    // 2. TÌM GIAO DỊCH THEO PARKING SESSION
    // =====================================

    Optional<Payment> findByParkingSession_Id(
            Long sessionId
    );


    // =====================================
    // 3. KIỂM TRA PHIÊN ĐÃ CÓ GIAO DỊCH CHƯA
    // =====================================

    boolean existsByParkingSession_Id(
            Long sessionId
    );


    // =====================================
    // 4. TÌM VÀ KHÓA GIAO DỊCH
    // Dùng khi xử lý thanh toán
    // =====================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.paymentCode = :paymentCode
    """)
    Optional<Payment> findByPaymentCodeForUpdate(
            @Param("paymentCode") String paymentCode
    );

    // =====================================
// 5. LẤY LỊCH SỬ THANH TOÁN
// =====================================

    List<Payment> findAllByOrderByCreatedAtDesc();
}
