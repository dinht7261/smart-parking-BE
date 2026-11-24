
package com.smartparking.backend.service;

import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.entity.Reservation;
import com.smartparking.backend.entity.User;

import com.smartparking.backend.enums.ParkingSpotStatus;
import com.smartparking.backend.enums.UserStatus;

import com.smartparking.backend.repository.ParkingSpotRepository;
import com.smartparking.backend.repository.ReservationRepository;
import com.smartparking.backend.repository.UserRepository;

import com.smartparking.backend.request.ReservationRequest;
import com.smartparking.backend.response.ReservationResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ParkingSpotRepository parkingSpotRepository,
            UserRepository userRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.userRepository = userRepository;
    }

    // =====================================
    // 1. TẠO ĐẶT CHỖ MỚI
    // =====================================

    @Transactional
    public ReservationResponse create(ReservationRequest request) {

        // Kiểm tra thời gian
        if (request.startTime() == null ||
                request.endTime() == null ||
                !request.endTime().isAfter(request.startTime())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian kết thúc phải sau thời gian bắt đầu"
            );
        }

        // Tìm tài khoản người dùng
        User user = userRepository.findByUserCode(
                request.userCode()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tài khoản người dùng"
        ));

        // Kiểm tra tài khoản còn hoạt động
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản hiện không hoạt động"
            );
        }

        // Tìm vị trí đỗ xe
        ParkingSpot spot = parkingSpotRepository.findByIdForUpdate(
                request.spotId()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy vị trí đỗ xe"
        ));

// Chỉ cho phép đặt vị trí AVAILABLE hoặc RESERVED
        if (spot.getStatus() != ParkingSpotStatus.AVAILABLE
                && spot.getStatus() != ParkingSpotStatus.RESERVED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí đỗ xe hiện không khả dụng"
            );
        }

        // Kiểm tra trùng lịch đặt chỗ
        long overlapping =
                reservationRepository.countOverlappingReservations(
                        spot.getId(),
                        request.startTime(),
                        request.endTime()
                );

        if (overlapping > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí đỗ xe đã có người đặt trong khoảng thời gian này"
            );
        }

        // Tạo mã đặt chỗ tự động
        String reservationCode = generateReservationCode();

        // Tạo đối tượng Reservation
        Reservation reservation = new Reservation(
                reservationCode,
                user.getFullName(),
                request.phone() == null || request.phone().isBlank()
                        ? null : request.phone().trim(),
                user.getLicensePlate(),
                spot,
                user,
                request.startTime(),
                request.endTime(),
                request.overnightRequested()
        );

// Lưu Reservation
        Reservation saved = reservationRepository.save(reservation);

// Cập nhật trạng thái vị trí
        spot.reserve();

        parkingSpotRepository.save(spot);

        // Trả kết quả
        return ReservationResponse.from(saved);
    }

    // =====================================
    // 2. LẤY LỊCH SỬ ĐẶT CHỖ CỦA USER
    // =====================================

    @Transactional(readOnly = true)
    public List<ReservationResponse> listByUserCode(String userCode) {

        User user = userRepository.findByUserCode(
                userCode
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tài khoản người dùng"
        ));

        return reservationRepository
                .findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // =====================================
    // 3. LẤY TOÀN BỘ DANH SÁCH ĐẶT CHỖ
    // =====================================

    @Transactional(readOnly = true)
    public List<ReservationResponse> listAll() {

        return reservationRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }


    // =====================================
    // 4. HỦY ĐẶT CHỖ
    // =====================================

    @Transactional
    public ReservationResponse cancel(
            String reservationCode,
            String userCode
    ) {

        // Kiểm tra mã đặt chỗ
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã đặt chỗ không được để trống"
            );
        }

        // Kiểm tra thông tin tài khoản
        if (userCode == null || userCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã người dùng không được để trống"
            );
        }

        // Tìm đặt chỗ theo mã
        Reservation reservation =
                reservationRepository.findByReservationCode(
                        reservationCode
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thông tin đặt chỗ"
                ));

        // Kiểm tra người thực hiện có phải chủ đặt chỗ
        if (!reservation.getUser().getUserCode()
                .equalsIgnoreCase(userCode.trim())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy đặt chỗ này"
            );
        }

        // Chỉ cho phép hủy PENDING hoặc CONFIRMED
        if (!reservation.getStatus().equals("PENDING")
                && !reservation.getStatus().equals("CONFIRMED")) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể hủy đặt chỗ ở trạng thái hiện tại"
            );
        }

        // Không cho phép hủy sau thời điểm bắt đầu
        if (!reservation.getStartTime()
                .isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể hủy sau thời điểm bắt đầu đặt chỗ"
            );
        }


// Cập nhật trạng thái đặt chỗ
        reservation.cancel();

// Lưu và đồng bộ xuống Database ngay
        Reservation saved =
                reservationRepository.saveAndFlush(reservation);

// Lấy vị trí đỗ xe
        ParkingSpot spot = reservation.getSpot();

// Kiểm tra còn lịch đặt chỗ nào hiệu lực không
        long remainingReservations =
                reservationRepository.countActiveReservationsForSpot(
                        spot.getId(),
                        LocalDateTime.now()
                );

// Chỉ giải phóng khi không còn lịch đặt
// và vị trí hiện đang ở trạng thái RESERVED
        if (remainingReservations == 0
                && spot.getStatus() == ParkingSpotStatus.RESERVED) {

            spot.makeAvailable();

            parkingSpotRepository.save(spot);
        }

        return ReservationResponse.from(saved);

    }


    // =====================================
    // 5. TẠO MÃ ĐẶT CHỖ
    // =====================================

    private String generateReservationCode() {

        return "RSV-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase(Locale.ROOT);
    }


}
