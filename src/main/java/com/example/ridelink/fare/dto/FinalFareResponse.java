package com.example.ridelink.fare.dto;

import com.example.ridelink.fare.entity.Fare;

import java.math.BigDecimal;

public record FinalFareResponse(String rideId, BigDecimal finalFare, String currency, double distanceKm,
                                long durationMinutes, String vehicleType, BigDecimal baseFare,
                                BigDecimal distanceCharge, BigDecimal timeCharge, BigDecimal multiplier) {
    public static FinalFareResponse from(Fare f) {
        return new FinalFareResponse(f.getRideId(), f.getTotal(), f.getCurrency(), f.getDistanceKm(),
                f.getDurationMinutes(), f.getVehicleType().name(), f.getBaseFare(), f.getDistanceCharge(),
                f.getTimeCharge(), f.getMultiplier());
    }
}
