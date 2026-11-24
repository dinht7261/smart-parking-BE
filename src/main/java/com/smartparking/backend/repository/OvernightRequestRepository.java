
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.OvernightRequest;
import com.smartparking.backend.enums.OvernightRequestStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OvernightRequestRepository
        extends JpaRepository<OvernightRequest, Long> {

    // =====================================
// KIỂM TRA YÊU CẦU QUA ĐÊM ĐÃ ĐƯỢC DUYỆT
// THEO TỪNG PHIÊN VÀ TỪNG NGÀY
// =====================================

    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM public.overnight_requests
        WHERE parking_session_id = :sessionId
          AND night_date = :nightDate
          AND status = 'APPROVED'
    )
    """, nativeQuery = true)
    boolean existsApprovedForSessionAndNight(
            @Param("sessionId") Long sessionId,
            @Param("nightDate") LocalDate nightDate
    );

    Optional<OvernightRequest> findByRequestCode(
            String requestCode
    );

    Optional<OvernightRequest> findByParkingSession_IdAndNightDate(
            Long sessionId,
            LocalDate nightDate
    );

    List<OvernightRequest> findByUser_IdOrderByNightDateDescCreatedAtDesc(
            Long userId
    );

    List<OvernightRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByParkingSession_IdAndNightDateAndStatus(
            Long sessionId,
            LocalDate nightDate,
            OvernightRequestStatus status
    );

    boolean existsByLicensePlateAndNightDateAndStatus(
            String licensePlate,
            LocalDate nightDate,
            OvernightRequestStatus status
    );
}
