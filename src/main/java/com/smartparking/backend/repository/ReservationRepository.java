
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    // Tìm đặt chỗ theo mã đặt chỗ
    Optional<Reservation> findByReservationCode(
            String reservationCode
    );

    // Lấy danh sách đặt chỗ của một người dùng
    List<Reservation> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // Lấy toàn bộ đặt chỗ mới nhất
    List<Reservation> findAllByOrderByCreatedAtDesc();

    // Kiểm tra vị trí đỗ xe có bị trùng lịch đặt hay không
    @Query("""
        SELECT COUNT(r)
        FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
          AND r.startTime < :endTime
          AND r.endTime > :startTime
    """)
    long countOverlappingReservations(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Đếm số lịch đặt chỗ còn hiệu lực của một vị trí
    @Query("""
        SELECT COUNT(r)
        FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.endTime > :now
    """)
    long countActiveReservationsForSpot(
            @Param("spotId") Long spotId,
            @Param("now") LocalDateTime now
    );


    // =====================================
    // 1. TÌM RESERVATION VÀ KHÓA BẢN GHI
    // =====================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.reservationCode = :reservationCode
    """)
    Optional<Reservation> findByReservationCodeForUpdate(
            @Param("reservationCode") String reservationCode
    );


    // =====================================
    // 2. KIỂM TRA VỊ TRÍ CÓ LỊCH ĐẶT
    // TRONG HIỆN TẠI HOẶC TƯƠNG LAI
    // =====================================

    boolean existsBySpot_IdAndStatusInAndEndTimeAfter(
            Long spotId,
            List<String> statuses,
            LocalDateTime checkInTime
    );


// =====================================
// 3. TÌM CÁC RESERVATION CÓ THỂ CHECK-IN
// THEO USER VÀ BIỂN SỐ XE
// =====================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT r
    FROM Reservation r
    WHERE r.user.id = :userId
      AND UPPER(r.licensePlate) = UPPER(:licensePlate)
      AND r.status IN ('PENDING', 'CONFIRMED')
      AND r.endTime >= :now
    ORDER BY r.startTime ASC
""")
    List<Reservation> findCheckInCandidatesForUpdate(
            @Param("userId") Long userId,
            @Param("licensePlate") String licensePlate,
            @Param("now") LocalDateTime now
    );

}
