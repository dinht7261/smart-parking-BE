package com.smartparking.backend.service;

import com.smartparking.backend.response.AvailabilityResponse;
import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.repository.ParkingSpotRepository;
import com.smartparking.backend.response.ParkingSpotResponse;
import com.smartparking.backend.enums.ParkingSpotStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository repository;

    public ParkingSpotService(ParkingSpotRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> list() {
        return repository.findAllByOrderByFloorCodeAscSpotCodeAsc()
            .stream()
            .map(ParkingSpotResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability() {
        List<ParkingSpot> spots = repository.findAll();
        return new AvailabilityResponse(
            spots.size(),
            count(spots, ParkingSpotStatus.AVAILABLE),
            count(spots, ParkingSpotStatus.RESERVED),
            count(spots, ParkingSpotStatus.OCCUPIED),
            count(spots, ParkingSpotStatus.MAINTENANCE)
        );
    }

    private long count(List<ParkingSpot> spots, ParkingSpotStatus status) {
        return spots.stream().filter(spot -> spot.getStatus() == status).count();
    }
}
