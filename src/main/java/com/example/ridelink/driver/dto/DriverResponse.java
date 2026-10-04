package com.example.ridelink.driver.dto;

import com.example.ridelink.driver.entity.DriverProfile;
import com.example.ridelink.driver.entity.DriverStatus;
import com.example.ridelink.driver.entity.VehicleDetails;

import java.time.Instant;

public record DriverResponse(String id, String licenseNumber, String serviceArea, DriverStatus status,
                             Double latitude, Double longitude, Instant locationUpdatedAt,
                             VehicleDetails vehicle) {
    public static DriverResponse from(DriverProfile d) {
        return new DriverResponse(d.getId(), d.getLicenseNumber(), d.getServiceArea(), d.getStatus(),
                d.getLatitude(), d.getLongitude(), d.getLocationUpdatedAt(), d.getVehicle());
    }
}
