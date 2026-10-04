package com.example.ridelink.fare.service;

import com.example.ridelink.fare.dto.EstimateRequest;
import com.example.ridelink.fare.dto.EstimateResponse;
import com.example.ridelink.fare.dto.FinalFareRequest;
import com.example.ridelink.fare.entity.Fare;
import com.example.ridelink.fare.exception.ApiException;
import com.example.ridelink.fare.repository.FareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FareServiceTest {

    private FareRepository repo;
    private FareService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(FareRepository.class);
        FareCalculator calc = new FareCalculator(new BigDecimal("100"), new BigDecimal("60"),
                new BigDecimal("5"), new BigDecimal("250"), 30.0, "LKR");
        service = new FareService(repo, calc);
        when(repo.save(any(Fare.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void estimate_usesCoordinates_andIsNotStored() {
        EstimateResponse r = service.estimate(new EstimateRequest(6.9271, 79.8612, 6.8389, 79.8653, "CAR"));
        assertTrue(r.distanceKm() > 9 && r.distanceKm() < 11);
        assertTrue(r.estimatedFare().compareTo(new BigDecimal("250")) > 0);
        assertEquals("LKR", r.currency());
        verify(repo, never()).save(any());
    }

    @Test
    void estimate_unknownVehicle_badRequest() {
        assertThrows(ApiException.class,
                () -> service.estimate(new EstimateRequest(6.9, 79.8, 6.8, 79.8, "BOAT")));
    }

    @Test
    void finalFare_isCalculatedAndStored() {
        when(repo.findByRideId("r1")).thenReturn(Optional.empty());
        Fare fare = service.finalFare(new FinalFareRequest("r1", 5.0, 15L, "CAR"));
        assertEquals(0, new BigDecimal("475.00").compareTo(fare.getTotal()));
        assertEquals("r1", fare.getRideId());
        verify(repo).save(any(Fare.class));
    }

    @Test
    void finalFare_isIdempotentPerRide() {
        Fare existing = new Fare();
        existing.setRideId("r1");
        existing.setTotal(new BigDecimal("999.00"));
        when(repo.findByRideId("r1")).thenReturn(Optional.of(existing));

        Fare fare = service.finalFare(new FinalFareRequest("r1", 5.0, 15L, "CAR"));

        assertSame(existing, fare);
        verify(repo, never()).save(any());
    }

    @Test
    void getByRide_missing_notFound() {
        when(repo.findByRideId("x")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.getByRide("x"));
        assertEquals(404, ex.getStatus().value());
    }
}
