
package com.smartparking.backend.service;

import com.smartparking.backend.entity.OvernightRequest;
import com.smartparking.backend.entity.ParkingSession;
import com.smartparking.backend.entity.User;

import com.smartparking.backend.enums.ClockType;
import com.smartparking.backend.enums.OvernightRequestStatus;
import com.smartparking.backend.enums.ParkingSessionStatus;

import com.smartparking.backend.repository.OvernightRequestRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.UserRepository;

import com.smartparking.backend.request.OvernightRequestRequest;
import com.smartparking.backend.response.OvernightRequestResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OvernightRequestService {

    private final OvernightRequestRepository requestRepository;
    private final ParkingSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SimulationClockService clockService;

    public OvernightRequestService(
            OvernightRequestRepository requestRepository,
            ParkingSessionRepository sessionRepository,
            UserRepository userRepository,
            SimulationClockService clockService
    ) {

        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.clockService = clockService;
    }


    // =====================================
    // 1. TẠO YÊU CẦU QUA ĐÊM
    // =====================================

    @Transactional
    public OvernightRequestResponse create(
            OvernightRequestRequest request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin yêu cầu không hợp lệ"
            );
        }

        // 1. LẤY USER

        String userCode = request.userCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        User user = userRepository.findByUserCode(
                userCode
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tài khoản người dùng"
        ));

        // 2. TÌM SESSION VÀ KHÓA BẢN GHI

        ParkingSession session =
                sessionRepository.findBySessionCodeForUpdate(
                        request.sessionCode().trim()
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiên gửi xe"
                ));

        // 3. KIỂM TRA SESSION CÒN ACTIVE

        if (session.getStatus() != ParkingSessionStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể đăng ký qua đêm khi xe đang gửi trong bãi"
            );
        }

        // 4. KIỂM TRA QUYỀN SỞ HỮU SESSION

        if (
                session.getUser() == null ||
                        !session.getUser().getId().equals(user.getId())
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Phiên gửi xe không thuộc tài khoản này"
            );
        }

        // 5. LẤY THỜI GIAN MÔ PHỎNG

        LocalDateTime now = clockService
                .getByType(ClockType.IN)
                .getSimulatedTime();

        // 6. KIỂM TRA KHUNG GIỜ ĐĂNG KÝ

        LocalDateTime nightEnd = request.nightDate()
                .plusDays(1)
                .atTime(6, 0);

        if (!nightEnd.isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khung giờ qua đêm này đã kết thúc"
            );
        }

        if (!nightEnd.isAfter(session.getCheckInTime())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày đăng ký không phù hợp với phiên gửi xe"
            );
        }

        // 7. KIỂM TRA YÊU CẦU ĐÃ TỒN TẠI CHƯA

        OvernightRequest existing =
                requestRepository
                        .findByParkingSession_IdAndNightDate(
                                session.getId(),
                                request.nightDate()
                        )
                        .orElse(null);

        if (existing != null) {

            // Đã duyệt: trả lại bản ghi cũ

            if (existing.getStatus() ==
                    OvernightRequestStatus.APPROVED) {

                return OvernightRequestResponse.from(existing);
            }

            // Đã hủy: cho phép đăng ký lại

            existing.approveAgain(
                    request.reason().trim(),
                    now
            );

            return OvernightRequestResponse.from(existing);
        }

        // 8. TẠO YÊU CẦU MỚI

        OvernightRequest newRequest = new OvernightRequest(

                generateRequestCode(),

                session,

                user,

                session.getLicensePlate(),

                request.nightDate(),

                request.reason().trim(),

                now
        );

        // Constructor đã tự động đặt APPROVED

        OvernightRequest saved =
                requestRepository.save(newRequest);

        return OvernightRequestResponse.from(saved);
    }


    // =====================================
    // 2. LẤY YÊU CẦU CỦA USER
    // =====================================

    @Transactional(readOnly = true)
    public List<OvernightRequestResponse> listByUserCode(
            String userCode
    ) {

        User user = userRepository.findByUserCode(
                userCode.trim()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tài khoản người dùng"
        ));

        return requestRepository
                .findByUser_IdOrderByNightDateDescCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(OvernightRequestResponse::from)
                .toList();
    }


    // =====================================
    // 3. LẤY TOÀN BỘ YÊU CẦU CHO OPERATOR
    // =====================================

    @Transactional(readOnly = true)
    public List<OvernightRequestResponse> listAll() {

        return requestRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OvernightRequestResponse::from)
                .toList();
    }


    // =====================================
    // 4. HỦY YÊU CẦU
    // =====================================

    @Transactional
    public OvernightRequestResponse cancel(
            String requestCode,
            String userCode
    ) {

        User user = userRepository.findByUserCode(
                userCode.trim()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy tài khoản người dùng"
        ));

        OvernightRequest request =
                requestRepository.findByRequestCode(
                        requestCode.trim()
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy yêu cầu qua đêm"
                ));

        if (!request.getUser().getId().equals(user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy yêu cầu này"
            );
        }

        LocalDateTime now = clockService
                .getByType(ClockType.IN)
                .getSimulatedTime();

        request.cancel(now);

        return OvernightRequestResponse.from(request);
    }


    // =====================================
    // 5. TẠO MÃ YÊU CẦU
    // =====================================

    private String generateRequestCode() {

        return "OVN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase(Locale.ROOT);
    }

}
