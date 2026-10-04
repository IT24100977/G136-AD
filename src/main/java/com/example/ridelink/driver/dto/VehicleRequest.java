package com.example.ridelink.driver.dto;

import com.example.ridelink.driver.entity.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(@NotBlank String plateNumber,
                             @NotBlank String make,
                             @NotBlank String model,
                             String color,
                             @NotNull @Min(1) @Max(12) Integer seats,
                             @NotNull VehicleType type) {
}
