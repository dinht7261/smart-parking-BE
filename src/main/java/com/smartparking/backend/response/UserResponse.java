
package com.smartparking.backend.response;

import com.smartparking.backend.entity.User;
import com.smartparking.backend.enums.UserRole;
import com.smartparking.backend.enums.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String userCode,
        String fullName,
        String licensePlate,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {

    public static UserResponse from(User source) {
        return new UserResponse(
                source.getId(),
                source.getUserCode(),
                source.getFullName(),
                source.getLicensePlate(),
                source.getRole(),
                source.getStatus(),
                source.getCreatedAt()
        );
    }
}
