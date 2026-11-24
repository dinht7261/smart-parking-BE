
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.SimulationClock;
import com.smartparking.backend.enums.ClockType;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SimulationClockRepository
        extends JpaRepository<SimulationClock, ClockType> {

    // Tìm đồng hồ theo loại IN hoặc OUT
    Optional<SimulationClock> findByClockType(
            ClockType clockType
    );

    // Khóa bản ghi khi cập nhật đồng hồ
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM SimulationClock c
        WHERE c.clockType = :clockType
    """)
    Optional<SimulationClock> findByClockTypeForUpdate(
            @Param("clockType") ClockType clockType
    );

    // Lấy danh sách hai đồng hồ
    List<SimulationClock> findAllByOrderByClockTypeAsc();
}
