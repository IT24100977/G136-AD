package com.example.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptResponse(String receiptNumber, String paymentId, String rideId, String passengerId,
                              String driverId, BigDecimal amount, String currency, String method, Instant paidAt,
                              String vehicleType, double distanceKm, long durationMinutes,
                              BigDecimal baseFare, BigDecimal distanceCharge, BigDecimal timeCharge,
                              BigDecimal multiplier) {
}
