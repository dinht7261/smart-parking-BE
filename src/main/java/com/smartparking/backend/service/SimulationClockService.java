
package com.smartparking.backend.service;

import com.smartparking.backend.entity.SimulationClock;
import com.smartparking.backend.enums.ClockType;
import com.smartparking.backend.repository.SimulationClockRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SimulationClockService {

    private final SimulationClockRepository repository;

    public SimulationClockService(
            SimulationClockRepository repository
    ) {
        this.repository = repository;
    }

    // =====================================
    // 1. LẤY TOÀN BỘ ĐỒNG HỒ
    // =====================================

    @Transactional
    public List<SimulationClock> getAll() {

        OffsetDateTime now = OffsetDateTime.now();

        List<SimulationClock> clocks = new ArrayList<>();

        for (ClockType type : ClockType.values()) {

            SimulationClock clock = getClockForUpdate(type);

            advanceClock(clock, now);

            clocks.add(repository.save(clock));
        }

        return clocks;
    }

    // =====================================
    // 2. LẤY MỘT ĐỒNG HỒ
    // =====================================

    @Transactional
    public SimulationClock getByType(ClockType type) {

        OffsetDateTime now = OffsetDateTime.now();

        SimulationClock clock = getClockForUpdate(type);

        advanceClock(clock, now);

        return repository.save(clock);
    }

    // =====================================
    // 3. BẮT ĐẦU ĐỒNG HỒ
    // =====================================

    @Transactional
    public SimulationClock start(ClockType type) {

        OffsetDateTime now = OffsetDateTime.now();

        SimulationClock clock = getClockForUpdate(type);

        advanceClock(clock, now);

        clock.start(now);

        return repository.save(clock);
    }

    // =====================================
    // 4. TẠM DỪNG ĐỒNG HỒ
    // =====================================

    @Transactional
    public SimulationClock pause(ClockType type) {

        OffsetDateTime now = OffsetDateTime.now();

        SimulationClock clock = getClockForUpdate(type);

        // Cập nhật thời gian trước khi dừng
        advanceClock(clock, now);

        clock.pause(now);

        return repository.save(clock);
    }

    // =====================================
    // 5. RESET ĐỒNG HỒ
    // =====================================

    @Transactional
    public SimulationClock reset(ClockType type) {

        OffsetDateTime now = OffsetDateTime.now();

        SimulationClock clock = getClockForUpdate(type);

        clock.reset(now);

        return repository.save(clock);
    }

    // =====================================
    // 6. ĐIỀU CHỈNH THỜI GIAN MÔ PHỎNG
    // =====================================

    @Transactional
    public SimulationClock setTime(
            ClockType type,
            LocalDateTime newTime
    ) {

        if (newTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian mô phỏng không được để trống"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        SimulationClock clock = getClockForUpdate(type);

        // Tính thời gian đã chạy trước khi điều chỉnh
        advanceClock(clock, now);

        clock.updateSimulatedTime(newTime, now);

        return repository.save(clock);
    }

    // =====================================
    // 7. ĐIỀU CHỈNH TỐC ĐỘ ĐỒNG HỒ
    // =====================================

    @Transactional
    public SimulationClock setSpeed(
            ClockType type,
            BigDecimal speedMultiplier
    ) {

        BigDecimal speed = validateSpeed(speedMultiplier);

        OffsetDateTime now = OffsetDateTime.now();

        SimulationClock clock = getClockForUpdate(type);

        // Tính thời gian theo tốc độ cũ trước
        advanceClock(clock, now);

        clock.updateSpeedMultiplier(speed, now);

        return repository.save(clock);
    }

    // =====================================
    // 8. TÌM ĐỒNG HỒ VÀ KHÓA BẢN GHI
    // =====================================

    private SimulationClock getClockForUpdate(
            ClockType type
    ) {

        if (type == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại đồng hồ không hợp lệ"
            );
        }

        return repository.findByClockTypeForUpdate(type)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đồng hồ " + type
                ));
    }

    // =====================================
    // 9. TÍNH THỜI GIAN MÔ PHỎNG
    // =====================================

    private void advanceClock(
            SimulationClock clock,
            OffsetDateTime realNow
    ) {

        // Nếu đồng hồ đang dừng thì không cập nhật
        if (!clock.isRunning()) {
            return;
        }

        OffsetDateTime lastRealTime = clock.getLastRealTime();

        // Nếu chưa có mốc thời gian thực
        if (lastRealTime == null) {

            clock.updateSimulatedTime(
                    clock.getSimulatedTime(),
                    realNow
            );

            return;
        }

        // Khoảng thời gian thực đã trôi qua
        Duration elapsed = Duration.between(
                lastRealTime,
                realNow
        );

        // Không cho thời gian chạy ngược
        if (elapsed.isNegative() || elapsed.isZero()) {

            clock.updateSimulatedTime(
                    clock.getSimulatedTime(),
                    realNow
            );

            return;
        }

        // Tính thời gian theo tốc độ mô phỏng
        long elapsedMillis = elapsed.toMillis();

        long simulatedMillis = BigDecimal
                .valueOf(elapsedMillis)
                .multiply(clock.getSpeedMultiplier())
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();

        // Cập nhật thời gian mô phỏng
        LocalDateTime newSimulatedTime =
                clock.getSimulatedTime()
                        .plus(Duration.ofMillis(simulatedMillis));

        clock.updateSimulatedTime(
                newSimulatedTime,
                realNow
        );
    }

    // =====================================
    // 10. KIỂM TRA TỐC ĐỘ ĐỒNG HỒ
    // =====================================

    private BigDecimal validateSpeed(
            BigDecimal speed
    ) {

        if (speed == null || speed.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tốc độ đồng hồ phải lớn hơn 0"
            );
        }

        try {

            speed = speed.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );

        } catch (ArithmeticException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tốc độ đồng hồ chỉ được tối đa 2 chữ số thập phân"
            );
        }

        if (speed.precision() > 8) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tốc độ đồng hồ vượt quá giới hạn cho phép"
            );
        }

        return speed;
    }
}
