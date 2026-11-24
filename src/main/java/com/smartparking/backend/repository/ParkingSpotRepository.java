package com.smartparking.backend.repository;

import com.smartparking.backend.enums.ParkingSpotStatus;
import com.smartparking.backend.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT p
    FROM ParkingSpot p
    WHERE p.id = :spotId
""")
    Optional<ParkingSpot> findByIdForUpdate(
            @Param("spotId") Long spotId
    );
    List<ParkingSpot> findAllByOrderByFloorCodeAscSpotCodeAsc();
    List<ParkingSpot> findByStatusOrderByFloorCodeAscSpotCodeAsc(
            ParkingSpotStatus status );
    // =====================================
// LẤY ID CÁC VỊ TRÍ THEO TRẠNG THÁI
// Không tải Entity trước khi khóa
// =====================================

    @Query("""
    SELECT p.id
    FROM ParkingSpot p
    WHERE p.status = :status
    ORDER BY p.floorCode ASC, p.spotCode ASC
""")
    List<Long> findIdsByStatusOrderByFloorCodeAscSpotCodeAsc(
            @Param("status") ParkingSpotStatus status
    );
}
