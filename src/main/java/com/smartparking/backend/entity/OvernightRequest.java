
package com.smartparking.backend.entity;

import com.smartparking.backend.enums.OvernightRequestStatus;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "overnight_requests",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_overnight_request_code",
                        columnNames = {"request_code"}
                ),
                @UniqueConstraint(
                        name = "uq_overnight_session_night",
                        columnNames = {
                                "parking_session_id",
                                "night_date"
                        }
                )
        }
)
public class OvernightRequest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "overnight_request_generator"
    )
    @SequenceGenerator(
            name = "overnight_request_generator",
            sequenceName = "overnight_request_seq",
            schema = "public",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(
            name = "request_code",
            nullable = false,
            length = 30
    )
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parking_session_id",
            nullable = false
    )
    private ParkingSession parkingSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "license_plate",
            nullable = false,
            length = 20
    )
    private String licensePlate;

    @Column(
            name = "night_date",
            nullable = false
    )
    private LocalDate nightDate;

    @Column(
            name = "reason",
            nullable = false,
            length = 500
    )
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private OvernightRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected OvernightRequest() {
    }

    public OvernightRequest(
            String requestCode,
            ParkingSession parkingSession,
            User user,
            String licensePlate,
            LocalDate nightDate,
            String reason,
            LocalDateTime registeredAt
    ) {

        this.requestCode = requestCode;
        this.parkingSession = parkingSession;
        this.user = user;
        this.licensePlate = licensePlate;
        this.nightDate = nightDate;
        this.reason = reason;

        // TỰ ĐỘNG PHÊ DUYỆT
        this.status = OvernightRequestStatus.APPROVED;

        this.createdAt = registeredAt;
        this.approvedAt = registeredAt;
    }

    public void cancel(LocalDateTime cancelledAt) {

        if (this.status != OvernightRequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Yêu cầu qua đêm không thể hủy"
            );
        }

        this.status = OvernightRequestStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }


    public void approveAgain(
            String reason,
            LocalDateTime approvedAt
    ) {

        if (this.status != OvernightRequestStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Chỉ có thể đăng ký lại yêu cầu đã hủy"
            );
        }

        this.reason = reason;
        this.status = OvernightRequestStatus.APPROVED;
        this.createdAt = approvedAt;
        this.approvedAt = approvedAt;
        this.cancelledAt = null;
    }


    public Long getId() {
        return id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public ParkingSession getParkingSession() {
        return parkingSession;
    }

    public User getUser() {
        return user;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public LocalDate getNightDate() {
        return nightDate;
    }

    public String getReason() {
        return reason;
    }

    public OvernightRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
