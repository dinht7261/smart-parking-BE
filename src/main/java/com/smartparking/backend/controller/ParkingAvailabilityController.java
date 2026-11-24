
package com.smartparking.backend.controller;

import com.smartparking.backend.response.ParkingAvailabilityResponse;
import com.smartparking.backend.service.ParkingAvailabilityService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/parking-availability")
public class ParkingAvailabilityController {

    private final ParkingAvailabilityService service;

    public ParkingAvailabilityController(
            ParkingAvailabilityService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ParkingAvailabilityResponse> getAvailability() {

        return ResponseEntity.ok(
                service.getAvailability()
        );
    }
}
