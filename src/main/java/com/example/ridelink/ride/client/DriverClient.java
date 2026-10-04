package com.example.ridelink.ride.client;

import java.util.List;

/** Port to the Driver & Vehicle Service (synchronous REST). */
public interface DriverClient {
    /** Eligible drivers nearest-first. */
    List<EligibleDriver> findEligible(double lat, double lng, String vehicleType);

    /** status: OFFLINE, AVAILABLE or ON_TRIP. */
    void updateStatus(String driverId, String status);
}
