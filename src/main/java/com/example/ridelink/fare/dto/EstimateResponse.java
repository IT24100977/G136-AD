package com.example.ridelink.fare.dto;

import java.math.BigDecimal;

public record EstimateResponse(BigDecimal estimatedFare, String currency, double distanceKm, long durationMinutes,
                               String vehicleType, BigDecimal baseFare, BigDecimal distanceCharge,
                               BigDecimal timeCharge, BigDecimal multiplier) {
}
