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
import com.example.ridelink.ride.exception.ApiException;
import com.example.ridelink.ride.repository.RideRepository;
import com.example.ridelink.ride.repository.RideStatusEventRepository;
import com.example.ridelink.ride.security.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RideServiceTest {

    private RideRepository rides;
    private RideStatusEventRepository events;
    private DriverClient driverClient;
    private FareClient fareClient;
    private RideService service;

    private final Actor passenger = new Actor("p1", "PASSENGER");
    private final Actor driver = new Actor("d1", "DRIVER");

    @BeforeEach
    void setUp() {
        rides = Mockito.mock(RideRepository.class);
        events = Mockito.mock(RideStatusEventRepository.class);
        driverClient = Mockito.mock(DriverClient.class);
        fareClient = Mockito.mock(FareClient.class);
        service = new RideService(rides, events, driverClient, fareClient);
        when(rides.save(any(Ride.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CreateRideRequest request() {
        return new CreateRideRequest("Fort", 6.9271, 79.8612, "Mount Lavinia", 6.8389, 79.8653, "CAR");
    }

    private Ride ride(RideStatus status) {
        Ride r = new Ride();
        r.setId("r1");
        r.setPassengerId("p1");
        r.setDriverId("d1");
        r.setPickupLatitude(6.9271);
        r.setPickupLongitude(79.8612);
        r.setDestinationLatitude(6.8389);
        r.setDestinationLongitude(79.8653);
        r.setVehicleType("CAR");
        r.setStatus(status);
        r.setStartedAt(Instant.now().minusSeconds(600));
        when(rides.findById("r1")).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void create_assignsNearestDriverAndReservesIt() {
        when(fareClient.estimate(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(new FareEstimate(new BigDecimal("850.00")));
        when(driverClient.findEligible(anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(new EligibleDriver("near", 0.5), new EligibleDriver("far", 3.0)));

        Ride ride = service.createRide("p1", request());

        assertEquals(RideStatus.ASSIGNED, ride.getStatus());
        assertEquals("near", ride.getDriverId());
        assertEquals(new BigDecimal("850.00"), ride.getEstimatedFare());
        verify(driverClient).updateStatus("near", "ON_TRIP");
    }

    @Test
    void create_noDriver_conflictAndNothingStored() {
        when(fareClient.estimate(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(new FareEstimate(BigDecimal.TEN));
        when(driverClient.findEligible(anyDouble(), anyDouble(), any())).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> service.createRide("p1", request()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(rides, never()).save(any());
        verify(driverClient, never()).updateStatus(any(), any());
    }

    @Test
    void create_fareServiceDown_propagatesAndDoesNotReserveDriver() {
        when(fareClient.estimate(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Fare service is unavailable"));

        ApiException ex = assertThrows(ApiException.class, () -> service.createRide("p1", request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        verify(driverClient, never()).updateStatus(any(), any());
    }

    @Test
    void accept_byOtherDriver_forbidden() {
        ride(RideStatus.ASSIGNED);
        ApiException ex = assertThrows(ApiException.class, () -> service.accept("r1", new Actor("d2", "DRIVER")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void accept_thenStart_followsLifecycle() {
        Ride r = ride(RideStatus.ASSIGNED);
        service.accept("r1", driver);
        assertEquals(RideStatus.ACCEPTED, r.getStatus());
        service.start("r1", driver);
        assertEquals(RideStatus.IN_PROGRESS, r.getStatus());
    }

    @Test
    void start_beforeAccept_invalidTransition() {
        ride(RideStatus.ASSIGNED);
        ApiException ex = assertThrows(ApiException.class, () -> service.start("r1", driver));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void complete_calculatesFareRecordsPaymentAndReleasesDriver() {
        Ride r = ride(RideStatus.IN_PROGRESS);
        when(fareClient.finalFare(eq("r1"), anyDouble(), anyLong(), any()))
                .thenReturn(new FinalFare(new BigDecimal("1200.00")));
        when(fareClient.recordPayment(eq("r1"), eq("p1"), eq("d1"), any()))
                .thenReturn(new PaymentResult("pay-1", "PAID"));

        Ride done = service.complete("r1", driver);

        assertEquals(RideStatus.COMPLETED, done.getStatus());
        assertEquals(new BigDecimal("1200.00"), done.getFinalFare());
        assertEquals("pay-1", done.getPaymentId());
        assertEquals(PaymentStatus.PAID, done.getPaymentStatus());
        assertNotNull(r.getDistanceKm());
        verify(driverClient).updateStatus("d1", "AVAILABLE");
    }

    @Test
    void complete_paymentFails_rideStillCompletedWithFailedPayment() {
        ride(RideStatus.IN_PROGRESS);
        when(fareClient.finalFare(any(), anyDouble(), anyLong(), any())).thenReturn(new FinalFare(BigDecimal.TEN));
        when(fareClient.recordPayment(any(), any(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Fare service is unavailable"));

        Ride done = service.complete("r1", driver);

        assertEquals(RideStatus.COMPLETED, done.getStatus());
        assertEquals(PaymentStatus.FAILED, done.getPaymentStatus());
    }

    @Test
    void complete_fareServiceDown_rideStaysInProgress() {
        Ride r = ride(RideStatus.IN_PROGRESS);
        when(fareClient.finalFare(any(), anyDouble(), anyLong(), any()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Fare service is unavailable"));

        assertThrows(ApiException.class, () -> service.complete("r1", driver));

        assertEquals(RideStatus.IN_PROGRESS, r.getStatus());
    }

    @Test
    void cancel_beforeStart_releasesDriver() {
        Ride r = ride(RideStatus.ASSIGNED);
        Ride cancelled = service.cancel("r1", passenger, "changed my mind");
        assertEquals(RideStatus.CANCELLED, cancelled.getStatus());
        assertEquals("changed my mind", r.getCancelReason());
        verify(driverClient).updateStatus("d1", "AVAILABLE");
    }

    @Test
    void cancel_completedRide_invalidTransition() {
        ride(RideStatus.COMPLETED);
        ApiException ex = assertThrows(ApiException.class, () -> service.cancel("r1", passenger, null));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void getRide_byStranger_forbidden_butAdminAllowed() {
        ride(RideStatus.ASSIGNED);
        assertThrows(ApiException.class, () -> service.getRide("r1", new Actor("x", "PASSENGER")));
        assertEquals("r1", service.getRide("r1", new Actor("a", "ADMIN")).getId());
    }

    @Test
    void getRide_missing_notFound() {
        when(rides.findById("nope")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.getRide("nope", passenger));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
