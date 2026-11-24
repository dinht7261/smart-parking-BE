
package com.smartparking.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", schema = "public")
public class Reservation {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "reservation_generator"
    )
    @SequenceGenerator(
            name = "reservation_generator",
            sequenceName = "reservation_seq",
            schema = "public",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reservation_code", nullable = false)
    private String reservationCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    // Liên kết với vị trí đỗ xe
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    // Liên kết với tài khoản người dùng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false)
    private String status;

    // Khách có đăng ký gửi xe qua đêm hay không
    @Column(name = "overnight_requested", nullable = false)
    private boolean overnightRequested;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructor mặc định dành cho JPA
    protected Reservation() {
    }

    // Constructor tạo đặt chỗ mới
    public Reservation(
            String reservationCode,
            String customerName,
            String phone,
            String licensePlate,
            ParkingSpot spot,
            User user,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean overnightRequested
    ) {
        this.reservationCode = reservationCode;
        this.customerName = customerName;
        this.phone = phone;
        this.licensePlate = licensePlate;
        this.spot = spot;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.overnightRequested = overnightRequested;

        this.status = "PENDING";
    }

    // Tự động thiết lập dữ liệu trước khi lưu
    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "PENDING";
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getReservationCode() {
        return reservationCode;
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

    public ParkingSpot getSpot() {
        return spot;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isOvernightRequested() {
        return overnightRequested;
    }
    public void cancel() {
        this.status = "CANCELLED";
    }

    // Khách đã đến và Check-in thành công
    public void markCheckedIn() {

        if (!status.equals("PENDING")
                && !status.equals("CONFIRMED")) {

            throw new IllegalStateException(
                    "Reservation không thể Check-in"
            );
        }

        this.status = "CHECKED_IN";
    }


    // Hoàn tất Reservation sau khi gửi xe
    public void markCompleted() {

        if (!status.equals("CHECKED_IN")) {

            throw new IllegalStateException(
                    "Reservation chưa được Check-in"
            );
        }

        this.status = "COMPLETED";
    }

}
