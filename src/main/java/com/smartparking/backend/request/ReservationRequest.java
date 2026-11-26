
package com.smartparking.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReservationRequest(

        @NotBlank(message = "User code không được để trống")
        @Size(max = 1, message = "User code không hợp lệ")
        String userCode,

        @NotNull(message = "Vui lòng chọn vị trí đỗ xe")
        @Positive(message = "Spot ID phải lớn hơn 0")
        Long spotId,

        @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
        LocalDateTime startTime,

        // Không bắt buộc.
        // Backend sẽ tự cập nhật khi Check-out thực tế.
        LocalDateTime endTime,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        boolean overnightRequested

) {
}
