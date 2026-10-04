package com.example.ridelink.driver.dto;

import com.example.ridelink.driver.entity.DriverProfile;
import com.example.ridelink.driver.entity.VehicleType;

public record EligibleDriverResponse(String driverId, double distanceKm, String serviceArea,
                                     VehicleType vehicleType, String plateNumber, String make, String model,
                                     Double latitude, Double longitude) {
    public static EligibleDriverResponse from(DriverProfile d, double distanceKm) {
        return new EligibleDriverResponse(d.getId(), Math.round(distanceKm * 100.0) / 100.0, d.getServiceArea(),
                d.getVehicle().getType(), d.getVehicle().getPlateNumber(), d.getVehicle().getMake(),
                d.getVehicle().getModel(), d.getLatitude(), d.getLongitude());
    }
}
