
package com.smartparking.backend.service;

import com.smartparking.backend.entity.Payment;
import com.smartparking.backend.entity.ParkingSession;
import com.smartparking.backend.entity.Reservation;
import com.smartparking.backend.entity.ParkingSpot;

import com.smartparking.backend.repository.ParkingSpotRepository;
import com.smartparking.backend.repository.ReservationRepository;
import com.smartparking.backend.enums.PaymentMethod;
import com.smartparking.backend.enums.PaymentStatus;
import com.smartparking.backend.enums.ParkingSessionStatus;

import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.OvernightRequestRepository;

import com.smartparking.backend.response.PaymentResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final ParkingSpotRepository spotRepository;

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingSessionRepository sessionRepository;
    private final OvernightRequestRepository overnightRequestRepository;

    // =====================================
    // GIÁ PHÍ
    // =====================================

    private static final BigDecimal HOURLY_RATE =
            BigDecimal.valueOf(5000);
    private static final BigDecimal RESERVATION_HOLD_HOURLY_RATE =
            BigDecimal.valueOf(5000);
    private static final BigDecimal OVERNIGHT_PENALTY =
            BigDecimal.valueOf(100000);


    // =====================================
    // CONSTRUCTOR
    // =====================================

    public PaymentService(
            OvernightRequestRepository overnightRequestRepository,
            PaymentRepository paymentRepository,
            ParkingSessionRepository sessionRepository,
            ParkingSpotRepository spotRepository,
            ReservationRepository reservationRepository
    ) {

        this.paymentRepository = paymentRepository;
        this.sessionRepository = sessionRepository;
        this.spotRepository = spotRepository;
        this.reservationRepository = reservationRepository;
        this.overnightRequestRepository = overnightRequestRepository;
    }


    // =====================================
    // 1. TẠO HÓA ĐƠN THANH TOÁN
    // =====================================

    @Transactional
    public Payment createPayment(String sessionCode) {

        if (sessionCode == null || sessionCode.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã phiên gửi xe không được để trống"
            );
        }

        // Tìm và khóa phiên gửi xe
        ParkingSession session =
                sessionRepository.findBySessionCodeForUpdate(
                        sessionCode.trim()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiên gửi xe"
                        )
                );

        // Tránh tạo hóa đơn trùng
        var existingPayment =
                paymentRepository.findByParkingSession_Id(
                        session.getId()
                );

        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        // Chỉ tạo hóa đơn khi đã Check-out
        if (session.getStatus()
                != ParkingSessionStatus.AWAITING_PAYMENT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiên gửi xe chưa sẵn sàng thanh toán"
            );
        }

        LocalDateTime checkInTime =
                session.getCheckInTime();

        LocalDateTime checkOutTime =
                session.getCheckOutTime();

        if (checkInTime == null || checkOutTime == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thiếu thời gian Check-in hoặc Check-out"
            );
        }

        // =====================================
        // TÍNH SỐ GIỜ GỬI XE
        // =====================================

        int billableHours = calculateBillableHours(
                checkInTime,
                checkOutTime
        );

        // =====================================
// TÍNH PHỤ PHÍ GIỮ CHỖ DO CHECK-IN MUỘN
// =====================================

        int reservationHoldHours =
                calculateReservationHoldHours(session);

        BigDecimal reservationHoldFee =
                RESERVATION_HOLD_HOURLY_RATE.multiply(
                        BigDecimal.valueOf(reservationHoldHours)
                );
        // =====================================
        // TÍNH SỐ ĐÊM VI PHẠM
        // KIỂM TRA TỪNG YÊU CẦU TRONG DATABASE
        // =====================================

        int violationNights =
                calculateViolationNights(session);

        // =====================================
        // TÍNH TIỀN GỬI XE
        // =====================================

        BigDecimal parkingFee =
                HOURLY_RATE.multiply(
                        BigDecimal.valueOf(billableHours)
                );

        // =====================================
        // TÍNH TIỀN PHẠT QUA ĐÊM
        // =====================================

        BigDecimal overnightPenalty =
                OVERNIGHT_PENALTY.multiply(
                        BigDecimal.valueOf(violationNights)
                );

        // =====================================
        // TỔNG TIỀN
        // =====================================

        BigDecimal totalAmount =
                parkingFee
                        .add(reservationHoldFee)
                        .add(overnightPenalty);

        // =====================================
        // TẠO GIAO DỊCH
        // =====================================

        Payment payment = new Payment(
                generatePaymentCode(),
                session,
                billableHours,
                parkingFee,
                reservationHoldHours,
                reservationHoldFee,
                violationNights,
                overnightPenalty,
                totalAmount,
                PaymentMethod.SIMULATED
        );
        return paymentRepository.save(payment);
    }


    // =====================================
    // 2. TÍNH SỐ GIỜ GỬI XE
    // =====================================

    private int calculateBillableHours(
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime
    ) {

        long seconds = Duration.between(
                checkInTime,
                checkOutTime
        ).getSeconds();

        if (seconds <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian gửi xe không hợp lệ"
            );
        }

        // Làm tròn lên giờ tiếp theo
        return Math.toIntExact(
                (seconds + 3599) / 3600
        );
    }


    // =====================================
    // TÍNH SỐ GIỜ CHECK-IN MUỘN
    // =====================================

    private int calculateReservationHoldHours(
            ParkingSession session
    ) {

        // Khách vãng lai không có Booking
        if (session.getReservation() == null) {
            return 0;
        }

        Reservation reservation =
                session.getReservation();

        LocalDateTime reservationStartTime =
                reservation.getStartTime();

        LocalDateTime checkInTime =
                session.getCheckInTime();

        if (reservationStartTime == null ||
                checkInTime == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thời gian Booking hoặc Check-in"
            );
        }

        // Check-in đúng giờ hoặc sớm hơn
        // Không tính phụ phí
        if (!checkInTime.isAfter(reservationStartTime)) {
            return 0;
        }

        // Tính thời gian khách đến muộn
        Duration delay = Duration.between(
                reservationStartTime,
                checkInTime
        );

        // Làm tròn lên giờ tiếp theo
        long fullHours = delay.toHours();

        boolean hasRemainingTime =
                !delay.minusHours(fullHours).isZero();

        return Math.toIntExact(
                fullHours + (hasRemainingTime ? 1 : 0)
        );
    }


    // =====================================
    // 3. TÍNH SỐ ĐÊM VI PHẠM
    // KIỂM TRA TỪNG NGÀY ĐĂNG KÝ
    // =====================================

    private int calculateViolationNights(
            ParkingSession session
    ) {

        LocalDateTime checkInTime =
                session.getCheckInTime();

        LocalDateTime checkOutTime =
                session.getCheckOutTime();

        if (checkInTime == null ||
                checkOutTime == null ||
                !checkOutTime.isAfter(checkInTime)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian gửi xe không hợp lệ"
            );
        }

        int violationNights = 0;

        // Ngày bắt đầu xét khung qua đêm
        LocalDate firstDate =
                checkInTime.toLocalDate().minusDays(1);

        // Ngày cuối cùng cần kiểm tra
        LocalDate lastDate =
                checkOutTime.toLocalDate();

        // Duyệt từng khung giờ qua đêm
        while (!firstDate.isAfter(lastDate)) {

            // Khung giờ 22:00 - 06:00
            LocalDateTime nightStart =
                    firstDate.atTime(22, 0);

            LocalDateTime nightEnd =
                    firstDate.plusDays(1).atTime(6, 0);

            // Kiểm tra phiên gửi xe có giao với
            // khung giờ qua đêm hay không
            boolean overlaps =
                    checkInTime.isBefore(nightEnd)
                            && checkOutTime.isAfter(nightStart);

            if (overlaps) {

                // night_date trong Database là
                // ngày kết thúc khung qua đêm
                //
                // Ví dụ:
                // 23/11 22:00 -> 24/11 06:00
                // night_date = 24/11

                LocalDate nightDate =
                        firstDate.plusDays(1);

                // Kiểm tra yêu cầu đã được tự động duyệt
                boolean approved =
                        overnightRequestRepository
                                .existsApprovedForSessionAndNight(
                                        session.getId(),
                                        nightDate
                                );

                // Không có yêu cầu được duyệt
                // => Tính một đêm vi phạm
                if (!approved) {

                    violationNights++;
                }
            }

            firstDate = firstDate.plusDays(1);
        }

        return violationNights;
    }


    // =====================================
    // 4. XÁC NHẬN THANH TOÁN MÔ PHỎNG
    // =====================================

    @Transactional
    public Payment confirmPayment(String paymentCode) {

        if (paymentCode == null || paymentCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã thanh toán không được để trống"
            );
        }

        // 1. Tìm và khóa giao dịch
        Payment payment =
                paymentRepository.findByPaymentCodeForUpdate(
                        paymentCode.trim()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy giao dịch"
                        )
                );

        // Đã thanh toán thành công
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Giao dịch không ở trạng thái chờ thanh toán"
            );
        }

        // 2. Lấy phiên gửi xe
        ParkingSession session = payment.getParkingSession();

        if (session.getStatus()
                != ParkingSessionStatus.AWAITING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiên gửi xe không sẵn sàng thanh toán"
            );
        }

        // 3. Xác nhận thanh toán
        payment.markSuccess(LocalDateTime.now());

        // 4. Hoàn tất phiên gửi xe
        session.complete();

        sessionRepository.save(session);

        // 5. Lưu giao dịch
        return paymentRepository.save(payment);
    }

    // =====================================
    // 5. SINH MÃ GIAO DỊCH
    // =====================================

    private String generatePaymentCode() {

        return "PAY-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }


    // =====================================
    // 6. TẠO HÓA ĐƠN VÀ TRẢ RESPONSE
    // =====================================

    @Transactional
    public PaymentResponse createPaymentResponse(
            String sessionCode
    ) {

        Payment payment = createPayment(sessionCode);

        return PaymentResponse.from(payment);
    }


    // =====================================
    // 7. XEM CHI TIẾT HÓA ĐƠN
    // =====================================

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentResponse(
            String paymentCode
    ) {

        if (paymentCode == null || paymentCode.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã thanh toán không được để trống"
            );
        }

        Payment payment =
                paymentRepository.findByPaymentCode(
                        paymentCode.trim()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy giao dịch"
                        )
                );

        return PaymentResponse.from(payment);
    }


    // =====================================
    // 8. XÁC NHẬN THANH TOÁN VÀ TRẢ RESPONSE
    // =====================================

    @Transactional
    public PaymentResponse confirmPaymentResponse(
            String paymentCode
    ) {

        Payment payment = confirmPayment(paymentCode);

        return PaymentResponse.from(payment);
    }


    // =====================================
    // 9. LẤY TOÀN BỘ LỊCH SỬ THANH TOÁN
    // =====================================

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory() {

        return paymentRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

}
