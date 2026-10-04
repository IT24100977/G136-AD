package com.example.ridelink.ride.entity;

import org.junit.jupiter.api.Test;

import static com.example.ridelink.ride.entity.RideStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class RideStatusTest {

    @Test
    void happyPath_isAllowed() {
        assertTrue(REQUESTED.canTransitionTo(ASSIGNED));
        assertTrue(ASSIGNED.canTransitionTo(ACCEPTED));
        assertTrue(ACCEPTED.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(COMPLETED));
    }

    @Test
    void cancellation_allowedOnlyBeforeTripStarts() {
        assertTrue(REQUESTED.canTransitionTo(CANCELLED));
        assertTrue(ASSIGNED.canTransitionTo(CANCELLED));
        assertTrue(ACCEPTED.canTransitionTo(CANCELLED));
        assertFalse(IN_PROGRESS.canTransitionTo(CANCELLED));
    }

    @Test
    void skippingStates_isRejected() {
        assertFalse(ASSIGNED.canTransitionTo(IN_PROGRESS));
        assertFalse(REQUESTED.canTransitionTo(COMPLETED));
        assertFalse(ACCEPTED.canTransitionTo(COMPLETED));
    }

    @Test
    void terminalStates_haveNoTransitions() {
        for (RideStatus next : RideStatus.values()) {
            assertFalse(COMPLETED.canTransitionTo(next));
            assertFalse(CANCELLED.canTransitionTo(next));
        }
    }
}
