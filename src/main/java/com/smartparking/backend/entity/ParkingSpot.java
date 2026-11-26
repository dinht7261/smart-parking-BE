
package com.smartparking.backend.entity;

import com.smartparking.backend.enums.ParkingSpotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "PARKING_SPOTS", uniqueConstraints = {
        @UniqueConstraint(
                name = "UQ_PARKING_SPOTS_LOCATION",
                columnNames = {"FLOOR_CODE", "SPOT_CODE"}
        )
})
public class ParkingSpot {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "parking_spot_generator"
    )
    @SequenceGenerator(
            name = "parking_spot_generator",
            sequenceName = "PARKING_SPOT_SEQ",
            allocationSize = 1
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "FLOOR_CODE", nullable = false, length = 10)
    private String floorCode;

    @Column(name = "SPOT_CODE", nullable = false, length = 20)
    private String spotCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ParkingSpotStatus status;

    protected ParkingSpot() {
    }

    // =====================================
    // 1. XÁC NHẬN XE ĐÃ ĐỖ
    // =====================================

    public void occupy() {

        if (status != ParkingSpotStatus.AVAILABLE
                && status != ParkingSpotStatus.RESERVED) {

            throw new IllegalStateException(
                    "Vị trí đỗ xe không khả dụng"
            );
        }

        this.status = ParkingSpotStatus.OCCUPIED;
    }

    // =====================================
    // 2. GIỮ VỊ TRÍ CHO KHÁCH
    // =====================================

    public void reserve() {

        if (status == ParkingSpotStatus.OCCUPIED ||
                status == ParkingSpotStatus.MAINTENANCE) {

            throw new IllegalStateException(
                    "Vị trí không thể đặt trước"
            );
        }

        this.status = ParkingSpotStatus.RESERVED;
    }

    // =====================================
    // 3. GIẢI PHÓNG VỊ TRÍ
    // =====================================

    public void release(boolean hasFutureReservations) {

        if (status != ParkingSpotStatus.OCCUPIED
                && status != ParkingSpotStatus.RESERVED) {

            throw new IllegalStateException(
                    "Vị trí không thể giải phóng ở trạng thái hiện tại"
            );
        }

        this.status = hasFutureReservations
                ? ParkingSpotStatus.RESERVED
                : ParkingSpotStatus.AVAILABLE;
    }

    public void release() {
        release(false);
    }

    // =====================================
    // 4. ĐƯA VỊ TRÍ VỀ TRỐNG
    // =====================================

    public void makeAvailable() {

        if (status != ParkingSpotStatus.MAINTENANCE) {
            this.status = ParkingSpotStatus.AVAILABLE;
        }
    }

    // =====================================
    // 5. GETTERS
    // =====================================

    public Long getId() {
        return id;
    }

    public String getFloorCode() {
        return floorCode;
    }

    public String getSpotCode() {
        return spotCode;
    }

    public ParkingSpotStatus getStatus() {
        return status;
    }
}
