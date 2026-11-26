
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    // =====================================
    // 1. TÌM RESERVATION
    // =====================================

    Optional<Reservation> findByReservationCode(
            String reservationCode
    );

    List<Reservation> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    List<Reservation> findAllByOrderByCreatedAtDesc();


    // =====================================
    // 2. KIỂM TRA VỊ TRÍ CÓ RESERVATION
    // =====================================

    boolean existsBySpot_IdAndStatusIn(
            Long spotId,
            Collection<String> statuses
    );


    // =====================================
    // 3. ĐẾM RESERVATION CÒN HIỆU LỰC
    // Không phụ thuộc endTime
    // =====================================

    @Query("""
        SELECT COUNT(r)
        FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status IN (
              'PENDING',
              'CONFIRMED',
              'CHECKED_IN'
          )
    """)
    long countActiveReservationsForSpot(
            @Param("spotId") Long spotId
    );


    // =====================================
    // 4. TÌM RESERVATION VÀ KHÓA BẢN GHI
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
    // 5. TÌM RESERVATION CÓ THỂ CHECK-IN
    // THEO USER VÀ BIỂN SỐ
    // Không lọc endTime vì endTime = NULL
    // =====================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.user.id = :userId
          AND UPPER(r.licensePlate) = UPPER(:licensePlate)
          AND r.status IN ('PENDING', 'CONFIRMED')
        ORDER BY r.startTime ASC
    """)
    List<Reservation> findCheckInCandidatesForUpdate(
            @Param("userId") Long userId,
            @Param("licensePlate") String licensePlate
    );


    // =====================================
    // 6. CÁC PHƯƠNG THỨC TƯƠNG THÍCH TẠM THỜI
    // Sẽ đồng bộ lại trong bước sửa ReservationService
    // =====================================

    default long countOverlappingReservations(
            Long spotId,
            LocalDateTime ignoredStartTime,
            LocalDateTime ignoredEndTime
    ) {
        return countActiveReservationsForSpot(spotId);
    }

    default long countActiveReservationsForSpot(
            Long spotId,
            LocalDateTime ignoredNow
    ) {
        return countActiveReservationsForSpot(spotId);
    }

    default boolean existsBySpot_IdAndStatusInAndEndTimeAfter(
            Long spotId,
            Collection<String> statuses,
            LocalDateTime ignoredCheckInTime
    ) {
        return existsBySpot_IdAndStatusIn(spotId, statuses);
    }

    default List<Reservation> findCheckInCandidatesForUpdate(
            Long userId,
            String licensePlate,
            LocalDateTime ignoredNow
    ) {
        return findCheckInCandidatesForUpdate(
                userId,
                licensePlate
        );
    }

}
