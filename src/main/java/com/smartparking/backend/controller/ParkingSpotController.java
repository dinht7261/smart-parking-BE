package com.smartparking.backend.controller;

import com.smartparking.backend.response.AvailabilityResponse;
import com.smartparking.backend.response.ParkingSpotResponse;
import com.smartparking.backend.service.ParkingSpotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
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
}
