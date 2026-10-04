package com.example.ridelink.ride.dto;

import com.example.ridelink.ride.entity.PaymentStatus;
import com.example.ridelink.ride.entity.Ride;
import com.example.ridelink.ride.entity.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RideResponse(String id, String passengerId, String driverId, RideStatus status,
                           String pickupName, double pickupLatitude, double pickupLongitude,
                           String destinationName, double destinationLatitude, double destinationLongitude,
                           String vehicleType, BigDecimal estimatedFare, BigDecimal finalFare, Double distanceKm,
                           String paymentId, PaymentStatus paymentStatus,
                           Instant requestedAt, Instant startedAt, Instant completedAt, Instant cancelledAt,
                           String cancelReason) {
    public static RideResponse from(Ride r) {
        return new RideResponse(r.getId(), r.getPassengerId(), r.getDriverId(), r.getStatus(),
                r.getPickupName(), r.getPickupLatitude(), r.getPickupLongitude(),
                r.getDestinationName(), r.getDestinationLatitude(), r.getDestinationLongitude(),
                r.getVehicleType(), r.getEstimatedFare(), r.getFinalFare(), r.getDistanceKm(),
                r.getPaymentId(), r.getPaymentStatus(),
                r.getRequestedAt(), r.getStartedAt(), r.getCompletedAt(), r.getCancelledAt(), r.getCancelReason());
    }
}
