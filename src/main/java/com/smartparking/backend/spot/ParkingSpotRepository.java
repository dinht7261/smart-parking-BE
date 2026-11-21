package com.smartparking.backend.spot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findAllByOrderByFloorCodeAscSpotCodeAsc();
}
