package com.example.ridelink.driver.controller;

import com.example.ridelink.driver.dto.AvailabilityRequest;
import com.example.ridelink.driver.dto.DriverRequest;
import com.example.ridelink.driver.dto.DriverResponse;
import com.example.ridelink.driver.dto.EligibleDriverResponse;
import com.example.ridelink.driver.dto.LocationRequest;
import com.example.ridelink.driver.dto.VehicleRequest;
import com.example.ridelink.driver.entity.VehicleType;
import com.example.ridelink.driver.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService service;

    public DriverController(DriverService service) {
        this.service = service;
    }

    /** Creates the caller's own driver profile; the id is the account id from the JWT. */
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponse> register(Authentication auth, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DriverResponse.from(service.register(auth.getName(), request)));
    }

    @GetMapping("/eligible")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EligibleDriverResponse>> eligible(@RequestParam double lat,
                                                                 @RequestParam double lng,
                                                                 @RequestParam(required = false) Double radiusKm,
                                                                 @RequestParam(required = false) VehicleType vehicleType,
                                                                 @RequestParam(required = false) String serviceArea,
                                                                 @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.findEligible(lat, lng, radiusKm, vehicleType, serviceArea, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DriverResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(DriverResponse.from(service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
    public ResponseEntity<DriverResponse> update(@PathVariable String id, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(DriverResponse.from(service.updateProfile(id, request)));
    }

    @PutMapping("/{id}/vehicle")
    @PreAuthorize("hasRole('DRIVER') and #id == authentication.name")
    public ResponseEntity<DriverResponse> vehicle(@PathVariable String id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(DriverResponse.from(service.saveVehicle(id, request)));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and #id == authentication.name)")
    public ResponseEntity<DriverResponse> availability(@PathVariable String id,
                                                       @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(DriverResponse.from(service.updateAvailability(id, request)));
    }

    @PutMapping("/{id}/location")
    @PreAuthorize("hasRole('DRIVER') and #id == authentication.name")
    public ResponseEntity<DriverResponse> location(@PathVariable String id, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(DriverResponse.from(service.updateLocation(id, request)));
    }
}
