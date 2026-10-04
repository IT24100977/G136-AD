package com.example.ridelink.fare.controller;

import com.example.ridelink.fare.dto.EstimateRequest;
import com.example.ridelink.fare.dto.EstimateResponse;
import com.example.ridelink.fare.dto.FinalFareRequest;
import com.example.ridelink.fare.dto.FinalFareResponse;
import com.example.ridelink.fare.service.FareService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fares")
public class FareController {

    private final FareService service;

    public FareController(FareService service) {
        this.service = service;
    }

    /** Any logged-in user can ask for an estimate. */
    @PostMapping("/estimate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EstimateResponse> estimate(@Valid @RequestBody EstimateRequest request) {
        return ResponseEntity.ok(service.estimate(request));
    }

    /** Called by the Ride Management Service (service token, role ADMIN) when a ride is completed. */
    @PostMapping("/final")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinalFareResponse> finalFare(@Valid @RequestBody FinalFareRequest request) {
        return ResponseEntity.ok(FinalFareResponse.from(service.finalFare(request)));
    }

    @GetMapping("/ride/{rideId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinalFareResponse> byRide(@PathVariable String rideId) {
        return ResponseEntity.ok(FinalFareResponse.from(service.getByRide(rideId)));
    }
}
