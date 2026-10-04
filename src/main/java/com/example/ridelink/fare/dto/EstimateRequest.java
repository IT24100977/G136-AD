package com.example.ridelink.fare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record EstimateRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double pickupLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double pickupLongitude,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double destinationLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double destinationLongitude,
        /** Optional: BIKE, TUK, CAR (default) or VAN. */
        String vehicleType) {
}
