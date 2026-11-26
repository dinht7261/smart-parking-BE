
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Thời gian dự kiến khách đến
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // Thời gian Check-out thực tế
    // NULL khi khách mới đặt chỗ
    @Column(name = "end_time", nullable = true)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "overnight_requested", nullable = false)
    private boolean overnightRequested;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Reservation() {
    }

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

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "PENDING";
        }
    }

    // =====================================
    // GETTERS
    // =====================================

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

    // =====================================
    // DUYỆT RESERVATION
    // =====================================

    public void confirm() {

        if (!"PENDING".equals(status)) {
            throw new IllegalStateException(
                    "Chỉ có thể duyệt Reservation đang chờ"
            );
        }

        this.status = "CONFIRMED";
    }

    // =====================================
    // TỪ CHỐI RESERVATION
    // =====================================

    public void reject() {

        if (!"PENDING".equals(status)) {
            throw new IllegalStateException(
                    "Chỉ có thể từ chối Reservation đang chờ"
            );
        }

        this.status = "REJECTED";
    }

    // =====================================
    // HỦY RESERVATION
    // =====================================

    public void cancel() {
        this.status = "CANCELLED";
    }

    // =====================================
    // CHECK-IN
    // =====================================

    public void markCheckedIn() {

        if (!"PENDING".equals(status)
                && !"CONFIRMED".equals(status)) {

            throw new IllegalStateException(
                    "Reservation không thể Check-in"
            );
        }

        this.status = "CHECKED_IN";
    }

    // =====================================
    // CHECK-OUT VÀ HOÀN TẤT
    // =====================================

    public void markCompleted(
            LocalDateTime actualCheckOutTime
    ) {

        if (!"CHECKED_IN".equals(status)) {
            throw new IllegalStateException(
                    "Reservation chưa được Check-in"
            );
        }

        if (actualCheckOutTime == null) {
            throw new IllegalArgumentException(
                    "Thời gian Check-out không hợp lệ"
            );
        }

        this.endTime = actualCheckOutTime;

        this.status = "COMPLETED";
    }

}
