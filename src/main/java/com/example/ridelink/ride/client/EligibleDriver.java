package com.example.ridelink.ride.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EligibleDriver(String driverId, double distanceKm) {
}
