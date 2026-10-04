package com.example.ridelink.fare.service;

import java.math.BigDecimal;

public record FareBreakdown(BigDecimal baseFare, BigDecimal distanceCharge, BigDecimal timeCharge,
                            BigDecimal multiplier, BigDecimal total) {
}
