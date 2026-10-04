package com.example.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverRequest(@NotBlank String licenseNumber, @NotBlank String serviceArea) {
}
