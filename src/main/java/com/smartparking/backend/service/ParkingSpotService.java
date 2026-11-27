
package com.smartparking.backend.service;

import com.smartparking.backend.response.AvailabilityResponse;
import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.repository.ParkingSpotRepository;
import com.smartparking.backend.repository.ReservationRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.response.ParkingSpotResponse;
import com.smartparking.backend.enums.ParkingSpotStatus;
import com.smartparking.backend.enums.ParkingSessionStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository repository;
    private final ReservationRepository reservationRepository;
    private final ParkingSessionRepository sessionRepository;

    public ParkingSpotService(
            ParkingSpotRepository repository,
            ReservationRepository reservationRepository,
            ParkingSessionRepository sessionRepository
    ) {
        this.repository = repository;
        this.reservationRepository = reservationRepository;
        this.sessionRepository = sessionRepository;
    }

    // =====================================
    // 1. LẤY DANH SÁCH VỊ TRÍ
    // =====================================

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> list() {
        return repository.findAllByOrderByFloorCodeAscSpotCodeAsc()
                .stream()
                .map(ParkingSpotResponse::from)
                .toList();
    }

    // =====================================
    // 2. THỐNG KÊ TRẠNG THÁI
    // =====================================

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

    // =====================================
    // 3. BẮT ĐẦU BẢO TRÌ
    // =====================================

    @Transactional
    public ParkingSpotResponse startMaintenance(Long spotId) {

        // Lấy và khóa vị trí đỗ

        ParkingSpot spot = repository.findByIdForUpdate(spotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy vị trí đỗ xe"
                ));

        // Chỉ cho phép bảo trì vị trí AVAILABLE

        if (spot.getStatus() != ParkingSpotStatus.AVAILABLE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể bảo trì vị trí đang trống"
            );
        }

        // Kiểm tra Reservation còn hiệu lực

        long activeReservations =
                reservationRepository.countActiveReservationsForSpot(
                        spotId
                );

        if (activeReservations > 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí đã có khách đặt chỗ, không thể bảo trì"
            );
        }

        // Kiểm tra xe đang sử dụng vị trí

        boolean hasActiveSession =
                sessionRepository.existsBySpot_IdAndStatusIn(
                        spotId,
                        List.of(ParkingSessionStatus.ACTIVE)
                );

        if (hasActiveSession) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí đang có xe, không thể bảo trì"
            );
        }

        // Chuyển trạng thái sang MAINTENANCE

        spot.startMaintenance();

        ParkingSpot saved = repository.saveAndFlush(spot);

        return ParkingSpotResponse.from(saved);
    }

    // =====================================
    // 4. KẾT THÚC BẢO TRÌ
    // =====================================

    @Transactional
    public ParkingSpotResponse endMaintenance(Long spotId) {

        ParkingSpot spot = repository.findByIdForUpdate(spotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy vị trí đỗ xe"
                ));

        if (spot.getStatus() != ParkingSpotStatus.MAINTENANCE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí không ở trạng thái bảo trì"
            );
        }

        spot.endMaintenance();

        ParkingSpot saved = repository.saveAndFlush(spot);

        return ParkingSpotResponse.from(saved);
    }

    // =====================================
    // 5. ĐẾM THEO TRẠNG THÁI
    // =====================================

    private long count(
            List<ParkingSpot> spots,
            ParkingSpotStatus status
    ) {
        return spots.stream()
                .filter(spot -> spot.getStatus() == status)
                .count();
    }
}
