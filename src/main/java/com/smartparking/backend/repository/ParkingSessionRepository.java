
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.ParkingSession;
import com.smartparking.backend.enums.ParkingSessionStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository
        extends JpaRepository<ParkingSession, Long> {

    // =====================================
    // 1. TÌM PHIÊN THEO MÃ
    // =====================================

    Optional<ParkingSession> findBySessionCode(
            String sessionCode
    );


    // =====================================
    // 2. TÌM PHIÊN VÀ KHÓA BẢN GHI
    // =====================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM ParkingSession s
        WHERE s.sessionCode = :sessionCode
    """)
    Optional<ParkingSession> findBySessionCodeForUpdate(
            @Param("sessionCode") String sessionCode
    );


    // =====================================
    // 3. KIỂM TRA XE ĐANG GỬI
    // =====================================

    Optional<ParkingSession>
    findFirstByLicensePlateAndStatusOrderByCheckInTimeDesc(
            String licensePlate,
            ParkingSessionStatus status
    );


    // =====================================
    // 4. KIỂM TRA BIỂN SỐ ĐÃ CÓ PHIÊN
    // =====================================

    boolean existsByLicensePlateAndStatusIn(
            String licensePlate,
            Collection<ParkingSessionStatus> statuses
    );

    boolean existsByUser_IdAndStatusIn(
            Long userId,
            Collection<ParkingSessionStatus> statuses
    );

    // =====================================
    // 5. LỊCH SỬ GỬI XE CỦA USER
    // =====================================

    List<ParkingSession> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // =====================================
// TÌM PHIÊN ĐANG GỬI THEO USER
// =====================================

    Optional<ParkingSession>
    findFirstByUser_IdAndStatusInOrderByCheckInTimeDesc(
            Long userId,
            Collection<ParkingSessionStatus> statuses
    );


    // =====================================
    // 6. TOÀN BỘ LỊCH SỬ GỬI XE
    // =====================================

    List<ParkingSession> findAllByOrderByCreatedAtDesc();


    // =====================================
    // 7. LẤY PHIÊN THEO TRẠNG THÁI
    // =====================================

    List<ParkingSession> findByStatusInOrderByCheckInTimeDesc(
            Collection<ParkingSessionStatus> statuses
    );


    // =====================================
    // 8. KIỂM TRA RESERVATION ĐÃ CÓ SESSION
    // =====================================

    boolean existsByReservation_Id(
            Long reservationId
    );


    // =====================================
    // 9. KIỂM TRA VỊ TRÍ ĐANG CÓ XE
    // =====================================

    boolean existsBySpot_IdAndStatusIn(
            Long spotId,
            Collection<ParkingSessionStatus> statuses
    );

}
