package com.example.ridelink.fare.dto;

import com.example.ridelink.fare.entity.Payment;
import com.example.ridelink.fare.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String paymentId, String rideId, String passengerId, String driverId,
                              BigDecimal amount, String currency, PaymentStatus status, String method,
                              String failureReason, String receiptNumber, Instant createdAt, Instant paidAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getRideId(), p.getPassengerId(), p.getDriverId(), p.getAmount(),
                p.getCurrency(), p.getStatus(), p.getMethod(), p.getFailureReason(), p.getReceiptNumber(),
                p.getCreatedAt(), p.getPaidAt());
    }
}
