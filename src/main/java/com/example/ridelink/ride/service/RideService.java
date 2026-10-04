package com.example.ridelink.ride.service;

import com.example.ridelink.ride.client.DriverClient;
import com.example.ridelink.ride.client.EligibleDriver;
import com.example.ridelink.ride.client.FareClient;
import com.example.ridelink.ride.client.FareEstimate;
import com.example.ridelink.ride.client.FinalFare;
import com.example.ridelink.ride.client.PaymentResult;
import com.example.ridelink.ride.dto.CreateRideRequest;
import com.example.ridelink.ride.entity.PaymentStatus;
import com.example.ridelink.ride.entity.Ride;
import com.example.ridelink.ride.entity.RideStatus;
import com.example.ridelink.ride.entity.RideStatusEvent;
import com.example.ridelink.ride.exception.ApiException;
import com.example.ridelink.ride.repository.RideRepository;
import com.example.ridelink.ride.repository.RideStatusEventRepository;
import com.example.ridelink.ride.security.Actor;
import com.example.ridelink.ride.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rides;
    private final RideStatusEventRepository events;
    private final DriverClient driverClient;
    private final FareClient fareClient;

    public RideService(RideRepository rides, RideStatusEventRepository events,
                       DriverClient driverClient, FareClient fareClient) {
        this.rides = rides;
        this.events = events;
        this.driverClient = driverClient;
        this.fareClient = fareClient;
    }

    /**
     * REQUESTED then ASSIGNED in one step: estimate the fare, ask the Driver service for eligible drivers,
     * pick the nearest, and reserve it (ON_TRIP). Nothing is stored if no driver is found.
     */
    public Ride createRide(String passengerId, CreateRideRequest req) {
        FareEstimate estimate = fareClient.estimate(req.pickupLatitude(), req.pickupLongitude(),
                req.destinationLatitude(), req.destinationLongitude(), req.vehicleType());

        List<EligibleDriver> drivers = driverClient.findEligible(req.pickupLatitude(), req.pickupLongitude(),
                req.vehicleType());
        if (drivers.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "No available driver near the pickup location");
        }
        EligibleDriver chosen = drivers.get(0); // nearest first
        driverClient.updateStatus(chosen.driverId(), "ON_TRIP");

        Ride ride = new Ride();
        ride.setPassengerId(passengerId);
        ride.setDriverId(chosen.driverId());
        ride.setPickupName(req.pickupName());
        ride.setPickupLatitude(req.pickupLatitude());
        ride.setPickupLongitude(req.pickupLongitude());
        ride.setDestinationName(req.destinationName());
        ride.setDestinationLatitude(req.destinationLatitude());
        ride.setDestinationLongitude(req.destinationLongitude());
        ride.setVehicleType(req.vehicleType());
        ride.setEstimatedFare(estimate.estimatedFare());
        ride.setRequestedAt(Instant.now());
        ride.setStatus(RideStatus.ASSIGNED);
        try {
            ride = rides.save(ride);
            record(ride, RideStatus.REQUESTED, passengerId);
            record(ride, RideStatus.ASSIGNED, "system");
        } catch (RuntimeException ex) {
            releaseDriver(chosen.driverId());
            throw ex;
        }
        return ride;
    }

    public Ride getRide(String id, Actor actor) {
        Ride ride = load(id);
        assertParticipantOrAdmin(ride, actor);
        return ride;
    }

    public List<RideStatusEvent> history(String id, Actor actor) {
        getRide(id, actor);
        return events.findByRideIdOrderByIdAsc(id);
    }

    public List<Ride> listMine(Actor actor) {
        return switch (actor.role()) {
            case "PASSENGER" -> rides.findByPassengerIdOrderByRequestedAtDesc(actor.id());
            case "DRIVER" -> rides.findByDriverIdOrderByRequestedAtDesc(actor.id());
            default -> rides.findAll();
        };
    }

    public Ride accept(String id, Actor actor) {
        Ride ride = load(id);
        assertAssignedDriver(ride, actor);
        requireTransition(ride, RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        return persist(ride, RideStatus.ACCEPTED, actor.id());
    }

    public Ride start(String id, Actor actor) {
        Ride ride = load(id);
        assertAssignedDriver(ride, actor);
        requireTransition(ride, RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        return persist(ride, RideStatus.IN_PROGRESS, actor.id());
    }

    /**
     * Final fare is calculated first: if the Fare service is down, the ride stays IN_PROGRESS and the call can
     * be retried. A failed payment does not undo the completed ride; it is recorded as paymentStatus FAILED.
     */
    public Ride complete(String id, Actor actor) {
        Ride ride = load(id);
        assertAssignedDriver(ride, actor);
        requireTransition(ride, RideStatus.COMPLETED);

        Instant now = Instant.now();
        double distance = GeoUtils.distanceKm(ride.getPickupLatitude(), ride.getPickupLongitude(),
                ride.getDestinationLatitude(), ride.getDestinationLongitude());
        long minutes = Math.max(1, Duration.between(ride.getStartedAt(), now).toMinutes());

        FinalFare fare = fareClient.finalFare(ride.getId(), distance, minutes, ride.getVehicleType());

        ride.setDistanceKm(Math.round(distance * 100.0) / 100.0);
        ride.setFinalFare(fare.finalFare());
        ride.setCompletedAt(now);
        ride = persist(ride, RideStatus.COMPLETED, actor.id());

        releaseDriver(ride.getDriverId());

        try {
            PaymentResult payment = fareClient.recordPayment(ride.getId(), ride.getPassengerId(),
                    ride.getDriverId(), ride.getFinalFare());
            ride.setPaymentId(payment.paymentId());
            ride.setPaymentStatus("PAID".equalsIgnoreCase(payment.status()) || "SUCCESS".equalsIgnoreCase(payment.status())
                    ? PaymentStatus.PAID : PaymentStatus.FAILED);
        } catch (ApiException ex) {
            log.warn("Payment recording failed for ride {}: {}", ride.getId(), ex.getMessage());
            ride.setPaymentStatus(PaymentStatus.FAILED);
        }
        return rides.save(ride);
    }

    public Ride cancel(String id, Actor actor, String reason) {
        Ride ride = load(id);
        assertParticipantOrAdmin(ride, actor);
        requireTransition(ride, RideStatus.CANCELLED);
        ride.setCancelledAt(Instant.now());
        ride.setCancelReason(reason);
        ride = persist(ride, RideStatus.CANCELLED, actor.id());
        if (ride.getDriverId() != null) {
            releaseDriver(ride.getDriverId());
        }
        return ride;
    }

    // ---- helpers

    private Ride load(String id) {
        return rides.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ride not found: " + id));
    }

    private void requireTransition(Ride ride, RideStatus next) {
        if (!ride.getStatus().canTransitionTo(next)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Invalid status transition: " + ride.getStatus() + " -> " + next);
        }
    }

    private Ride persist(Ride ride, RideStatus next, String by) {
        ride.setStatus(next);
        Ride saved = rides.save(ride);
        record(saved, next, by);
        return saved;
    }

    private void record(Ride ride, RideStatus status, String by) {
        events.save(new RideStatusEvent(ride.getId(), status, by));
    }

    private void releaseDriver(String driverId) {
        try {
            driverClient.updateStatus(driverId, "AVAILABLE");
        } catch (ApiException ex) {
            log.warn("Could not release driver {}: {}", driverId, ex.getMessage());
        }
    }

    private void assertAssignedDriver(Ride ride, Actor actor) {
        if (!actor.id().equals(ride.getDriverId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the assigned driver can perform this action");
        }
    }

    private void assertParticipantOrAdmin(Ride ride, Actor actor) {
        boolean participant = actor.id().equals(ride.getPassengerId()) || actor.id().equals(ride.getDriverId());
        if (!participant && !actor.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this ride");
        }
    }
}
