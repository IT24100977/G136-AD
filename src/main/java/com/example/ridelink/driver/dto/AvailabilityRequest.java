package com.example.ridelink.driver.dto;

import com.example.ridelink.driver.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(@NotNull DriverStatus status) {
}
