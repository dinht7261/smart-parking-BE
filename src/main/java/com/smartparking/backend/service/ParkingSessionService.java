
package com.smartparking.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartparking.backend.entity.ParkingSession;
import com.smartparking.backend.entity.ParkingSpot;
import com.smartparking.backend.entity.Reservation;
import com.smartparking.backend.entity.SimulationClock;
import com.smartparking.backend.entity.User;

import com.smartparking.backend.enums.ClockType;
import com.smartparking.backend.enums.ParkingSessionStatus;
import com.smartparking.backend.enums.ParkingSessionType;
import com.smartparking.backend.enums.ParkingSpotStatus;
import com.smartparking.backend.enums.UserStatus;

import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSpotRepository;
import com.smartparking.backend.repository.ReservationRepository;

import com.smartparking.backend.request.ParkingSessionCheckInRequest;
import com.smartparking.backend.response.ParkingSessionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ParkingSessionService {

    @Value("${parking.qr.in-payload}")
    private String expectedInQrPayload;

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository spotRepository;
    private final ReservationRepository reservationRepository;
    private final SimulationClockService clockService;

    // =====================================
    // TRẠNG THÁI PHIÊN CHƯA HOÀN TẤT
    // =====================================

    private static final List<ParkingSessionStatus>
            UNFINISHED_SESSION_STATUSES =
            List.of(
                    ParkingSessionStatus.ACTIVE,
                    ParkingSessionStatus.AWAITING_PAYMENT
            );

    // Chỉ ACTIVE mới chiếm vị trí thực tế.

    private static final List<ParkingSessionStatus>
            PARKING_SPOT_OCCUPYING_STATUSES =
            List.of(
                    ParkingSessionStatus.ACTIVE
            );

    // Những Reservation đang giữ chỗ.

    private static final List<String>
            BLOCKING_RESERVATION_STATUSES =
            List.of(
                    "PENDING",
                    "CONFIRMED",
                    "CHECKED_IN"
            );

    // =====================================
    // CONSTRUCTOR
    // =====================================

    public ParkingSessionService(
            ParkingSessionRepository sessionRepository,
            ParkingSpotRepository spotRepository,
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            SimulationClockService clockService,
            PaymentService paymentService
    ) {

        this.sessionRepository = sessionRepository;
        this.spotRepository = spotRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.clockService = clockService;
        this.paymentService = paymentService;
    }

    // =====================================
    // 1. CHECK-IN CHUNG
    // =====================================

    @Transactional
    public ParkingSessionResponse checkIn(
            ParkingSessionCheckInRequest request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin Check-in không hợp lệ"
            );
        }

        // 1. XÁC THỰC QR CỔNG IN

        if (request.qrPayload() == null
                || expectedInQrPayload == null
                || !expectedInQrPayload.equals(
                request.qrPayload().trim()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Mã QR cổng IN không hợp lệ"
            );
        }

        // 2. XÁC ĐỊNH USER

        String userCode = requireText(
                request.userCode(),
                "Vui lòng chọn tài khoản người dùng"
        ).toUpperCase(Locale.ROOT);

        User user = userRepository.findByUserCode(
                userCode
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tài khoản người dùng"
        ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản hiện không hoạt động"
            );
        }

        // 3. LẤY BIỂN SỐ TỪ DATABASE

        String licensePlate = normalizeLicensePlate(
                user.getLicensePlate()
        );

        if (request.licensePlate() != null
                && !request.licensePlate().isBlank()) {

            String requestPlate = normalizeLicensePlate(
                    request.licensePlate()
            );

            if (!requestPlate.equals(licensePlate)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Biển số không thuộc tài khoản này"
                );
            }
        }

        // 4. LẤY THỜI GIAN IN CLOCK

        LocalDateTime checkInTime =
                getSimulatedTime(ClockType.IN);

        // 5. KIỂM TRA USER ĐÃ CÓ PHIÊN GỬI XE CHƯA

        if (sessionRepository.existsByUser_IdAndStatusIn(
                user.getId(),
                UNFINISHED_SESSION_STATUSES
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tài khoản này đang có xe trong bãi"
            );
        }

        // 6. KIỂM TRA BIỂN SỐ ĐÃ CÓ PHIÊN GỬI XE CHƯA

        if (sessionRepository.existsByLicensePlateAndStatusIn(
                licensePlate,
                UNFINISHED_SESSION_STATUSES
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Xe này đang có phiên gửi xe chưa hoàn tất"
            );
        }

        // 7. TÌM RESERVATION CỦA USER
        // Không sử dụng endTime vì endTime = NULL
        // cho đến khi khách thực sự Check-out.

        List<Reservation> candidates =
                reservationRepository.findCheckInCandidatesForUpdate(
                        user.getId(),
                        licensePlate
                );

        Reservation matchedReservation = null;

        boolean hasFutureReservation = false;

        for (Reservation reservation : candidates) {

            // Khách được phép đến sớm tối đa 30 phút
            LocalDateTime earliestAllowedCheckIn =
                    reservation.getStartTime().minusMinutes(30);

            // Nếu đến sớm hơn 30 phút thì chưa được Check-in
            if (checkInTime.isBefore(earliestAllowedCheckIn)) {
                hasFutureReservation = true;
                continue;
            }

            // Tìm thấy Reservation hợp lệ
            if (matchedReservation != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "User có nhiều lịch đặt chỗ cùng thời điểm"
                );
            }

            matchedReservation = reservation;
        }

        // 8. CÓ RESERVATION PHÙ HỢP

        if (matchedReservation != null) {

            return checkInReservation(
                    user,
                    matchedReservation,
                    checkInTime
            );
        }

        // 9. CÓ RESERVATION TRONG TƯƠNG LAI

        if (hasFutureReservation) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ được Check-in sớm tối đa 30 phút so với giờ đặt chỗ"
            );
        }

        // 10. KHÔNG CÓ RESERVATION
        // => KHÁCH VÃNG LAI

        return checkInWalkIn(
                user,
                request,
                checkInTime,
                licensePlate
        );
    }

    // =====================================
    // 2. CHECK-IN KHÁCH VÃNG LAI
    // =====================================

    private ParkingSessionResponse checkInWalkIn(
            User user,
            ParkingSessionCheckInRequest request,
            LocalDateTime checkInTime,
            String licensePlate
    ) {

        // 1. LẤY DANH SÁCH VỊ TRÍ TRỐNG

        List<ParkingSpot> availableSpots =
                spotRepository.findByStatusOrderByFloorCodeAscSpotCodeAsc(
                        ParkingSpotStatus.AVAILABLE
                );

        ParkingSpot selectedSpot = null;

        // 2. TÌM VỊ TRÍ PHÙ HỢP

        for (ParkingSpot candidate : availableSpots) {

            ParkingSpot spot =
                    spotRepository.findByIdForUpdate(
                            candidate.getId()
                    ).orElse(null);

            if (spot == null) {
                continue;
            }

            if (spot.getStatus() != ParkingSpotStatus.AVAILABLE) {
                continue;
            }

            // KIỂM TRA VỊ TRÍ ĐÃ CÓ XE CHƯA

            boolean occupied =
                    sessionRepository.existsBySpot_IdAndStatusIn(
                            spot.getId(),
                            PARKING_SPOT_OCCUPYING_STATUSES
                    );

            if (occupied) {
                continue;
            }

            // KIỂM TRA VỊ TRÍ ĐÃ CÓ RESERVATION CHƯA
            // Không sử dụng endTime.

            boolean reserved =
                    reservationRepository.existsBySpot_IdAndStatusIn(
                            spot.getId(),
                            BLOCKING_RESERVATION_STATUSES
                    );

            if (reserved) {
                continue;
            }

            selectedSpot = spot;

            break;
        }

        // 3. KIỂM TRA CÒN CHỖ KHÔNG

        if (selectedSpot == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hiện không còn vị trí đỗ xe khả dụng"
            );
        }

        // 4. GIỮ VỊ TRÍ CHO KHÁCH
        // Chờ cảm biến xác nhận xe đã đỗ.

        selectedSpot.reserve();

        spotRepository.save(selectedSpot);

        // 5. TẠO PARKING SESSION

        ParkingSession session = new ParkingSession(
                generateSessionCode(),
                ParkingSessionType.WALK_IN,
                user.getFullName(),
                normalizePhone(request.phone()),
                licensePlate,
                selectedSpot,
                user,
                null,
                checkInTime,
                request.overnightRequested()
        );

        // 6. LƯU DATABASE

        ParkingSession saved =
                sessionRepository.save(session);

        return ParkingSessionResponse.from(saved);
    }

    // =====================================
    // 3. CHECK-IN KHÁCH ĐÃ ĐẶT CHỖ
    // =====================================

    private ParkingSessionResponse checkInReservation(
            User user,
            Reservation reservation,
            LocalDateTime checkInTime
    ) {

        // 1. KIỂM TRA ĐÃ CHECK-IN CHƯA

        if (sessionRepository.existsByReservation_Id(
                reservation.getId()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Đặt chỗ này đã được Check-in"
            );
        }

        // 2. KIỂM TRA TRẠNG THÁI

        if (!"PENDING".equals(reservation.getStatus())
                && !"CONFIRMED".equals(reservation.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Đặt chỗ không cho phép Check-in"
            );
        }

        // 3. KIỂM TRA USER SỞ HỮU RESERVATION

        if (reservation.getUser() == null
                || !reservation.getUser().getId().equals(
                user.getId()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Đặt chỗ không thuộc tài khoản này"
            );
        }

        // 4. KIỂM TRA BIỂN SỐ

        String licensePlate = normalizeLicensePlate(
                user.getLicensePlate()
        );

        String reservationPlate = normalizeLicensePlate(
                reservation.getLicensePlate()
        );

        if (!licensePlate.equals(reservationPlate)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Biển số đặt chỗ không khớp với tài khoản"
            );
        }

// 5. KIỂM TRA THỜI GIAN CHECK-IN

// Cho phép khách đến sớm tối đa 30 phút
        LocalDateTime earliestAllowedCheckIn =
                reservation.getStartTime().minusMinutes(30);

        if (checkInTime.isBefore(earliestAllowedCheckIn)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ được Check-in sớm tối đa 30 phút so với giờ đặt chỗ"
            );
        }

        // Check-in muộn vẫn được phép.
        // Giữ nguyên startTime để PaymentService
        // tính reservationHoldHours và reservationHoldFee.

        // 6. LẤY VỊ TRÍ ĐÃ ĐẶT VÀ KHÓA BẢN GHI

        ParkingSpot spot =
                spotRepository.findByIdForUpdate(
                        reservation.getSpot().getId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy vị trí đã đặt"
                        )
                );

        // 7. KIỂM TRA TRẠNG THÁI VỊ TRÍ

        if (spot.getStatus() != ParkingSpotStatus.AVAILABLE
                && spot.getStatus() != ParkingSpotStatus.RESERVED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí đặt trước hiện không khả dụng"
            );
        }

        // 8. KIỂM TRA VỊ TRÍ ĐÃ CÓ XE CHƯA

        if (sessionRepository.existsBySpot_IdAndStatusIn(
                spot.getId(),
                PARKING_SPOT_OCCUPYING_STATUSES
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vị trí đã có xe khác sử dụng"
            );
        }

        // 9. KIỂM TRA BIỂN SỐ ĐÃ CÓ PHIÊN CHƯA

        if (sessionRepository.existsByLicensePlateAndStatusIn(
                licensePlate,
                UNFINISHED_SESSION_STATUSES
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Xe này đang có phiên gửi xe chưa hoàn tất"
            );
        }

        // 10. GIỮ VỊ TRÍ ĐÃ ĐẶT

        spot.reserve();

        spotRepository.save(spot);

        // 11. CẬP NHẬT RESERVATION

        reservation.markCheckedIn();

        reservationRepository.save(reservation);

        // 12. TẠO PARKING SESSION

        ParkingSession session = new ParkingSession(
                generateSessionCode(),
                ParkingSessionType.RESERVATION,
                reservation.getCustomerName(),
                reservation.getPhone(),
                licensePlate,
                spot,
                user,
                reservation,
                checkInTime,
                reservation.isOvernightRequested()
        );

        // 13. LƯU DATABASE

        ParkingSession saved =
                sessionRepository.save(session);

        return ParkingSessionResponse.from(saved);
    }

    // =====================================
    // 4. CHECK-OUT
    // =====================================

    @Transactional
    public ParkingSessionResponse checkOut(
            String sessionCode
    ) {

        if (sessionCode == null || sessionCode.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã phiên gửi xe không được để trống"
            );
        }

        // 1. TÌM VÀ KHÓA PARKING SESSION

        ParkingSession session =
                sessionRepository.findBySessionCodeForUpdate(
                        sessionCode.trim()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiên gửi xe"
                        )
                );

        if (session.getStatus() != ParkingSessionStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiên gửi xe không ở trạng thái ACTIVE"
            );
        }

        // 2. LẤY THỜI GIAN OUT CLOCK

        LocalDateTime checkOutTime =
                getSimulatedTime(ClockType.OUT);

        if (!checkOutTime.isAfter(
                session.getCheckInTime()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thời gian OUT Clock phải sau thời gian IN Clock"
            );
        }

        // 3. CHUYỂN SESSION SANG CHỜ THANH TOÁN

        session.checkOut(checkOutTime);

        // 4. HOÀN TẤT RESERVATION NẾU CÓ

        Reservation reservation = session.getReservation();

        if (reservation != null) {

            reservation.markCompleted(checkOutTime);

            reservationRepository.save(reservation);
        }

        // 5. KHÓA VỊ TRÍ ĐỖ

        ParkingSpot spot =
                spotRepository.findByIdForUpdate(
                        session.getSpot().getId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy vị trí đỗ xe"
                        )
                );

        // 6. KIỂM TRA RESERVATION KHÁC CÒN HIỆU LỰC
        // Reservation hiện tại đã chuyển COMPLETED.
        // Không sử dụng endTime để kiểm tra nữa.

        long remainingReservations =
                reservationRepository.countActiveReservationsForSpot(
                        spot.getId()
                );

        // 7. GIẢI PHÓNG VỊ TRÍ

        // Có Reservation khác => RESERVED
        // Không còn Reservation => AVAILABLE

        spot.release(remainingReservations > 0);

        spotRepository.save(spot);

        // 8. LƯU PARKING SESSION

        ParkingSession saved =
                sessionRepository.save(session);

        // 9. TẠO HÓA ĐƠN

        paymentService.createPayment(
                sessionCode.trim()
        );

        return ParkingSessionResponse.from(saved);
    }

    // =====================================
    // 5. LẤY DANH SÁCH XE ĐANG GỬI
    // =====================================

    @Transactional(readOnly = true)
    public List<ParkingSessionResponse> getActiveSessions() {

        return sessionRepository
                .findByStatusInOrderByCheckInTimeDesc(
                        UNFINISHED_SESSION_STATUSES
                )
                .stream()
                .map(ParkingSessionResponse::from)
                .toList();
    }

    // =====================================
    // 6. LẤY LỊCH SỬ GỬI XE
    // =====================================

    @Transactional(readOnly = true)
    public List<ParkingSessionResponse> getSessionHistory() {

        return sessionRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ParkingSessionResponse::from)
                .toList();
    }

    // =====================================
    // 7. TRA CỨU PHIÊN GỬI XE
    // =====================================

    @Transactional(readOnly = true)
    public ParkingSessionResponse getSessionByCode(
            String sessionCode
    ) {

        if (sessionCode == null || sessionCode.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã phiên gửi xe không được để trống"
            );
        }

        ParkingSession session =
                sessionRepository.findBySessionCode(
                        sessionCode.trim()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiên gửi xe"
                        )
                );

        return ParkingSessionResponse.from(session);
    }

    // =====================================
    // 8. LẤY THỜI GIAN SIMULATION CLOCK
    // =====================================

    private LocalDateTime getSimulatedTime(
            ClockType clockType
    ) {

        SimulationClock clock =
                clockService.getByType(clockType);

        return clock.getSimulatedTime();
    }

    // =====================================
    // 9. CHUẨN HÓA BIỂN SỐ
    // =====================================

    private String normalizeLicensePlate(
            String licensePlate
    ) {

        if (licensePlate == null || licensePlate.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biển số xe không được để trống"
            );
        }

        return licensePlate
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    // =====================================
    // 10. CHUẨN HÓA SỐ ĐIỆN THOẠI
    // =====================================

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    // =====================================
    // 11. KIỂM TRA DỮ LIỆU BẮT BUỘC
    // =====================================

    private String requireText(
            String value,
            String message
    ) {

        if (value == null || value.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }

        return value.trim();
    }

    // =====================================
    // 12. TẠO MÃ PHIÊN
    // =====================================

    private String generateSessionCode() {

        return "SES-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase(Locale.ROOT);
    }
}
