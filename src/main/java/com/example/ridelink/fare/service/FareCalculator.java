package com.example.ridelink.fare.service;

import com.example.ridelink.fare.entity.VehicleType;
import com.example.ridelink.fare.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The documented fare rule:
 *   subtotal = baseFare + (perKm x distanceKm) + (perMinute x durationMinutes)
 *   total    = max(minimumFare, subtotal x vehicleMultiplier), rounded to 2 decimals
 */
@Component
public class FareCalculator {

    private final BigDecimal baseFare;
    private final BigDecimal perKm;
    private final BigDecimal perMinute;
    private final BigDecimal minimumFare;
    private final double averageSpeedKmh;
    private final String currency;

    public FareCalculator(@Value("${app.fare.base-fare}") BigDecimal baseFare,
                          @Value("${app.fare.per-km}") BigDecimal perKm,
                          @Value("${app.fare.per-minute}") BigDecimal perMinute,
                          @Value("${app.fare.minimum-fare}") BigDecimal minimumFare,
                          @Value("${app.fare.average-speed-kmh}") double averageSpeedKmh,
                          @Value("${app.fare.currency}") String currency) {
        this.baseFare = baseFare;
        this.perKm = perKm;
        this.perMinute = perMinute;
        this.minimumFare = minimumFare;
        this.averageSpeedKmh = averageSpeedKmh;
        this.currency = currency;
    }

    public FareBreakdown calculate(double distanceKm, long durationMinutes, VehicleType type) {
        if (distanceKm < 0 || durationMinutes < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Distance and duration must not be negative");
        }
        BigDecimal base = baseFare.setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceCharge = perKm.multiply(BigDecimal.valueOf(distanceKm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeCharge = perMinute.multiply(BigDecimal.valueOf(durationMinutes)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = base.add(distanceCharge).add(timeCharge);

        BigDecimal total = subtotal.multiply(type.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minimum = minimumFare.setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(minimum) < 0) {
            total = minimum;
        }
        return new FareBreakdown(base, distanceCharge, timeCharge, type.getMultiplier(), total);
    }

    /** Estimated trip time from straight-line distance and the configured average speed. */
    public long estimateDurationMinutes(double distanceKm) {
        return Math.max(1, Math.round(distanceKm / averageSpeedKmh * 60.0));
    }

    /** Blank or missing means CAR. */
    public VehicleType parseVehicleType(String value) {
        if (value == null || value.isBlank()) {
            return VehicleType.CAR;
        }
        try {
            return VehicleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown vehicle type: " + value);
        }
    }

    public String currency() {
        return currency;
    }
}
