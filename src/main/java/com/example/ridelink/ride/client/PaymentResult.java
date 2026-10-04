package com.example.ridelink.ride.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** ASSUMED Fare & Payment contract: adjust field names to match the real service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResult(String paymentId, String status) {
}
