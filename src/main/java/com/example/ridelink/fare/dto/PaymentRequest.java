package com.example.ridelink.fare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(@NotBlank String rideId,
                             @NotBlank String passengerId,
                             @NotBlank String driverId,
                             @NotNull @DecimalMin("0.01") BigDecimal amount,
                             /** Optional demo switch: forces the simulated payment to be declined. */
                             Boolean simulateFailure) {
}
