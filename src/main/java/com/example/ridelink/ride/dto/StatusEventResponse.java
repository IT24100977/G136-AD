package com.example.ridelink.ride.dto;

import com.example.ridelink.ride.entity.RideStatus;
import com.example.ridelink.ride.entity.RideStatusEvent;

import java.time.Instant;

public record StatusEventResponse(RideStatus status, Instant changedAt, String changedBy) {
    public static StatusEventResponse from(RideStatusEvent e) {
        return new StatusEventResponse(e.getStatus(), e.getChangedAt(), e.getChangedBy());
    }
}
