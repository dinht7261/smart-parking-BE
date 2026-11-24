
package com.smartparking.backend.service;

import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.repository.ParkingSpotRepository;
import com.smartparking.backend.response.ParkingAvailabilityResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParkingAvailabilityService {

    private final ParkingSpotRepository spotRepository;

    public ParkingAvailabilityService(
            ParkingSpotRepository spotRepository
    ) {
        this.spotRepository = spotRepository;
    }

    @Transactional(readOnly = true)
    public ParkingAvailabilityResponse getAvailability() {

        // Đọc các vị trí thực tế từ PostgreSQL
        List<ParkingSpot> spots =
                spotRepository.findAllByOrderByFloorCodeAscSpotCodeAsc();

        long available = 0;
        long reserved = 0;
        long occupied = 0;
        long maintenance = 0;

        // Đếm theo trạng thái
        for (ParkingSpot spot : spots) {

            switch (spot.getStatus()) {

                case AVAILABLE -> available++;

                case RESERVED -> reserved++;

                case OCCUPIED -> occupied++;

                case MAINTENANCE -> maintenance++;
            }
        }

        long total = spots.size();

        // Nội dung hiển thị trên OLED
        String displayText = available + "/" + total;

        return new ParkingAvailabilityResponse(
                total,
                available,
                reserved,
                occupied,
                maintenance,
                displayText
        );
    }
}
