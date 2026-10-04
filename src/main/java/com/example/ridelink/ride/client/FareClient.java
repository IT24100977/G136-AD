package com.example.ridelink.ride.client;

import java.math.BigDecimal;

/** Port to the Fare & Payment Service (synchronous REST). */
public interface FareClient {
    FareEstimate estimate(double pickupLat, double pickupLng, double destLat, double destLng, String vehicleType);

    FinalFare finalFare(String rideId, double distanceKm, long durationMinutes, String vehicleType);

    PaymentResult recordPayment(String rideId, String passengerId, String driverId, BigDecimal amount);
}
