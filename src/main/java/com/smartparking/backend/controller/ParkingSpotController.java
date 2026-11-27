package com.smartparking.backend.controller;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.smartparking.backend.response.AvailabilityResponse;
import com.smartparking.backend.response.ParkingSpotResponse;
import com.smartparking.backend.service.ParkingSpotService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1/parking-spots")
public class ParkingSpotController {

    private final ParkingSpotService service;

    public ParkingSpotController(ParkingSpotService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParkingSpotResponse> list() {
        return service.list();
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability() {
        return service.availability();
    }


    // =====================================
    // 3. BẮT ĐẦU BẢO TRÌ VỊ TRÍ
    // =====================================

    @PutMapping("/{spotId}/maintenance")
    public ParkingSpotResponse startMaintenance(
            @PathVariable Long spotId
    ) {
        return service.startMaintenance(spotId);
    }

    // =====================================
    // 4. KẾT THÚC BẢO TRÌ VỊ TRÍ
    // =====================================

    @PutMapping("/{spotId}/maintenance/end")
    public ParkingSpotResponse endMaintenance(
            @PathVariable Long spotId
    ) {
        return service.endMaintenance(spotId);
    }

}
