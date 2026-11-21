package com.smartparking.backend.spot;

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
    @UniqueConstraint(name = "UQ_PARKING_SPOTS_LOCATION", columnNames = {"FLOOR_CODE", "SPOT_CODE"})
})
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parking_spot_generator")
    @SequenceGenerator(name = "parking_spot_generator", sequenceName = "PARKING_SPOT_SEQ", allocationSize = 1)
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

    public Long getId() { return id; }
    public String getFloorCode() { return floorCode; }
    public String getSpotCode() { return spotCode; }
    public ParkingSpotStatus getStatus() { return status; }
}
