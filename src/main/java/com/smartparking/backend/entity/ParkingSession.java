
package com.smartparking.backend.entity;

import com.smartparking.backend.enums.ParkingSessionStatus;
import com.smartparking.backend.enums.ParkingSessionType;
import java.util.UUID;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "parking_sessions",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_parking_sessions_session_code",
                        columnNames = {"session_code"}
                ),
                @UniqueConstraint(
                        name = "uq_parking_sessions_reservation",
                        columnNames = {"reservation_id"}
                )
        }
)
public class ParkingSession {

    // =====================================
    // 1. ID
    // =====================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "parking_session_generator"
    )
    @SequenceGenerator(
            name = "parking_session_generator",
            sequenceName = "parking_session_seq",
            schema = "public",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false)
    private Long id;


    // =====================================
    // 2. MÃ PHIÊN GỬI XE
    // =====================================

    @Column(
            name = "session_code",
            nullable = false,
            length = 30
    )
    private String sessionCode;

    // QR Token riêng cho từng phiên gửi xe
    @Column(
            name = "exit_qr_token",
            nullable = false,
            unique = true,
            length = 64
    )
    private String exitQrToken;

    // =====================================
    // 3. LOẠI PHIÊN GỬI XE
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "session_type",
            nullable = false,
            length = 20
    )
    private ParkingSessionType sessionType;


    // =====================================
    // 4. THÔNG TIN KHÁCH HÀNG
    // =====================================

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(
            name = "license_plate",
            nullable = false,
            length = 20
    )
    private String licensePlate;

    // Khách có đăng ký gửi xe qua đêm hay không
    @Column(name = "overnight_requested", nullable = false)
    private boolean overnightRequested;

    // =====================================
    // 5. LIÊN KẾT USER
    // Khách vãng lai có thể không có user
    // =====================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    // =====================================
    // 6. LIÊN KẾT RESERVATION
    // Khách vãng lai không có reservation
    // =====================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reservation_id",
            unique = true
    )
    private Reservation reservation;


    // =====================================
    // 7. LIÊN KẾT VỊ TRÍ ĐỖ XE
    // =====================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "spot_id",
            nullable = false
    )
    private ParkingSpot spot;


    // =====================================
    // 8. THỜI GIAN CHECK-IN / CHECK-OUT
    // =====================================

    @Column(
            name = "check_in_time",
            nullable = false
    )
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;


    // =====================================
    // 9. TRẠNG THÁI PHIÊN GỬI XE
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ParkingSessionStatus status;


    // =====================================
    // 10. THỜI GIAN TẠO VÀ CẬP NHẬT
    // =====================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    // =====================================
    // CONSTRUCTOR MẶC ĐỊNH CHO JPA
    // =====================================

    protected ParkingSession() {
    }


    // =====================================
    // CONSTRUCTOR TẠO PHIÊN GỬI XE
    // =====================================

    public ParkingSession(
            String sessionCode,
            ParkingSessionType sessionType,
            String customerName,
            String phone,
            String licensePlate,
            ParkingSpot spot,
            User user,
            Reservation reservation,
            LocalDateTime checkInTime,
            boolean overnightRequested
    ) {

        this.sessionCode = sessionCode;
        this.exitQrToken = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
        this.sessionType = sessionType;
        this.customerName = customerName;
        this.phone = phone;
        this.licensePlate = licensePlate;
        this.spot = spot;
        this.user = user;
        this.reservation = reservation;
        this.checkInTime = checkInTime;
        this.overnightRequested = overnightRequested;

        this.status = ParkingSessionStatus.ACTIVE;
    }


    // =====================================
    // TỰ ĐỘNG GHI NHẬN THỜI GIAN
    // =====================================

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = ParkingSessionStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }


    // =====================================
    // CHECK-OUT
    // =====================================

    public void checkOut(LocalDateTime time) {

        if (status != ParkingSessionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Phiên gửi xe không ở trạng thái ACTIVE"
            );
        }

        if (time == null || !time.isAfter(checkInTime)) {
            throw new IllegalArgumentException(
                    "Thời gian Check-out phải sau Check-in"
            );
        }

        this.checkOutTime = time;

        this.status = ParkingSessionStatus.AWAITING_PAYMENT;
    }


    // =====================================
    // HOÀN TẤT THANH TOÁN
    // =====================================

    public void complete() {

        if (status != ParkingSessionStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException(
                    "Phiên gửi xe chưa sẵn sàng hoàn tất"
            );
        }

        this.status = ParkingSessionStatus.COMPLETED;
    }


    // =====================================
    // HỦY PHIÊN GỬI XE
    // =====================================

    public void cancel() {

        if (status != ParkingSessionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Không thể hủy phiên gửi xe hiện tại"
            );
        }

        this.status = ParkingSessionStatus.CANCELLED;
    }


    // =====================================
    // GETTERS
    // =====================================

    public Long getId() {
        return id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public ParkingSessionType getSessionType() {
        return sessionType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public User getUser() {
        return user;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public ParkingSessionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOvernightRequested() {
        return overnightRequested;
    }

    public String getExitQrToken() {return exitQrToken;}
}
