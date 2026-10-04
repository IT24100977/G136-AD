package com.example.ridelink.ride.controller;

import com.example.ridelink.ride.dto.CancelRequest;
import com.example.ridelink.ride.dto.CreateRideRequest;
import com.example.ridelink.ride.dto.RideResponse;
import com.example.ridelink.ride.dto.StatusEventResponse;
import com.example.ridelink.ride.security.Actor;
import com.example.ridelink.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rides")
public class RideController {

    private final RideService service;

    public RideController(RideService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> create(Authentication auth, @Valid @RequestBody CreateRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RideResponse.from(service.createRide(auth.getName(), request)));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RideResponse>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(Actor.from(auth)).stream().map(RideResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RideResponse> get(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(RideResponse.from(service.getRide(id, Actor.from(auth))));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StatusEventResponse>> history(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(service.history(id, Actor.from(auth)).stream().map(StatusEventResponse::from).toList());
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> accept(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(RideResponse.from(service.accept(id, Actor.from(auth))));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> start(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(RideResponse.from(service.start(id, Actor.from(auth))));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> complete(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(RideResponse.from(service.complete(id, Actor.from(auth))));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RideResponse> cancel(Authentication auth, @PathVariable String id,
                                               @Valid @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(RideResponse.from(service.cancel(id, Actor.from(auth), reason)));
    }
}
