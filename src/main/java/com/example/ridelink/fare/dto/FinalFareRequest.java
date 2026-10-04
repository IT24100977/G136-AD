package com.example.ridelink.fare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinalFareRequest(@NotBlank String rideId,
                               @NotNull @DecimalMin("0.0") Double distanceKm,
                               @NotNull @Min(1) Long durationMinutes,
                               String vehicleType) {
}
