
package com.smartparking.backend.entity;

import com.smartparking.backend.enums.PaymentMethod;
import com.smartparking.backend.enums.PaymentStatus;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payments_payment_code",
                        columnNames = {"payment_code"}
                ),
                @UniqueConstraint(
                        name = "uq_payments_session",
                        columnNames = {"session_id"}
                )
        }
)
public class Payment {

    // =====================================
    // 1. ID
    // =====================================

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "payment_generator"
    )
    @SequenceGenerator(
            name = "payment_generator",
            sequenceName = "payment_seq",
            schema = "public",
            allocationSize = 1
    )
    @Column(name = "id", nullable = false)
    private Long id;


    // =====================================
    // 2. MÃ GIAO DỊCH
    // =====================================

    @Column(
            name = "payment_code",
            nullable = false,
            length = 30
    )
    private String paymentCode;


    // =====================================
    // 3. LIÊN KẾT PARKING SESSION
    // Mỗi phiên chỉ có một Payment
    // =====================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false
    )
    private ParkingSession parkingSession;


    // =====================================
    // 4. SỐ TIỀN THANH TOÁN
    // =====================================

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 0
    )
    private BigDecimal amount;

    // Số giờ gửi xe được tính phí
    @Column(name = "billable_hours", nullable = false)
    private int billableHours;

    // Tiền gửi xe
    @Column(name = "parking_fee", nullable = false, precision = 15, scale = 0)
    private BigDecimal parkingFee;

    // Số đêm vi phạm
    @Column(name = "violation_nights", nullable = false)
    private int violationNights;

    // Tổng tiền phạt qua đêm
    @Column(name = "overnight_penalty", nullable = false, precision = 15, scale = 0)
    private BigDecimal overnightPenalty;

    // =====================================
    // 5. PHƯƠNG THỨC THANH TOÁN
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private PaymentMethod paymentMethod;


    // =====================================
    // 6. TRẠNG THÁI THANH TOÁN
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private PaymentStatus status;


    // =====================================
    // 7. THỜI GIAN THANH TOÁN
    // =====================================

    @Column(name = "paid_at")
    private LocalDateTime paidAt;


    // =====================================
    // 8. THỜI GIAN TẠO VÀ CẬP NHẬT
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

    protected Payment() {
    }


    // =====================================
    // CONSTRUCTOR TẠO GIAO DỊCH
    // =====================================

    public Payment(
            String paymentCode,
            ParkingSession parkingSession,
            int billableHours,
            BigDecimal parkingFee,
            int violationNights,
            BigDecimal overnightPenalty,
            BigDecimal amount,
            PaymentMethod paymentMethod
    ) {

        if (paymentCode == null || paymentCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã thanh toán không được để trống"
            );
        }

        if (parkingSession == null) {
            throw new IllegalArgumentException(
                    "Phiên gửi xe không được để trống"
            );
        }

        if (billableHours <= 0 || violationNights < 0) {
            throw new IllegalArgumentException(
                    "Thông tin thời gian tính phí không hợp lệ"
            );
        }

        if (parkingFee == null || parkingFee.signum() < 0
                || overnightPenalty == null
                || overnightPenalty.signum() < 0) {
            throw new IllegalArgumentException(
                    "Chi tiết chi phí không hợp lệ"
            );
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán phải lớn hơn 0"
            );
        }

        if (amount.compareTo(
                parkingFee.add(overnightPenalty)
        ) != 0) {
            throw new IllegalArgumentException(
                    "Tổng tiền không khớp chi tiết hóa đơn"
            );
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ"
            );
        }

        this.paymentCode = paymentCode;
        this.parkingSession = parkingSession;

        this.billableHours = billableHours;
        this.parkingFee = parkingFee;
        this.violationNights = violationNights;
        this.overnightPenalty = overnightPenalty;

        this.amount = amount;
        this.paymentMethod = paymentMethod;

        this.status = PaymentStatus.PENDING;
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
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }


    // =====================================
    // XÁC NHẬN THANH TOÁN THÀNH CÔNG
    // =====================================

    public void markSuccess(LocalDateTime time) {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Giao dịch không ở trạng thái chờ thanh toán"
            );
        }

        if (time == null) {
            throw new IllegalArgumentException(
                    "Thời gian thanh toán không hợp lệ"
            );
        }

        this.status = PaymentStatus.SUCCESS;
        this.paidAt = time;
    }


    // =====================================
    // THANH TOÁN THẤT BẠI
    // =====================================

    public void markFailed() {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Không thể cập nhật trạng thái giao dịch"
            );
        }

        this.status = PaymentStatus.FAILED;
    }


    // =====================================
    // HỦY GIAO DỊCH
    // =====================================

    public void cancel() {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Không thể hủy giao dịch hiện tại"
            );
        }

        this.status = PaymentStatus.CANCELLED;
    }


    // =====================================
    // GETTERS
    // =====================================

    public Long getId() {
        return id;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public ParkingSession getParkingSession() {
        return parkingSession;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getBillableHours() {
        return billableHours;
    }

    public BigDecimal getParkingFee() {
        return parkingFee;
    }

    public int getViolationNights() {
        return violationNights;
    }

    public BigDecimal getOvernightPenalty() {
        return overnightPenalty;
    }
}
