package com.example.ridelink.driver.service;

import com.example.ridelink.driver.dto.AvailabilityRequest;
import com.example.ridelink.driver.dto.DriverRequest;
import com.example.ridelink.driver.dto.EligibleDriverResponse;
import com.example.ridelink.driver.entity.DriverProfile;
import com.example.ridelink.driver.entity.DriverStatus;
import com.example.ridelink.driver.entity.VehicleDetails;
import com.example.ridelink.driver.entity.VehicleType;
import com.example.ridelink.driver.exception.ApiException;
import com.example.ridelink.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DriverServiceTest {

    private DriverRepository repo;
    private DriverService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(DriverRepository.class);
        service = new DriverService(repo, 5, 30);
        when(repo.save(any(DriverProfile.class))).thenAnswer(i -> i.getArgument(0));
    }

    private DriverProfile driver(String id, double lat, double lng, VehicleType type, Instant locAt) {
        DriverProfile d = new DriverProfile();
        d.setId(id);
        d.setLicenseNumber("L-" + id);
        d.setServiceArea("Colombo");
        d.setStatus(DriverStatus.AVAILABLE);
        d.setLatitude(lat);
        d.setLongitude(lng);
        d.setLocationUpdatedAt(locAt);
        VehicleDetails v = new VehicleDetails();
        v.setPlateNumber("PL-" + id);
        v.setType(type);
        d.setVehicle(v);
        return d;
    }

    @Test
    void register_duplicateAccount_conflict() {
        when(repo.existsById("u1")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.register("u1", new DriverRequest("L1", "Colombo")));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void register_newDriver_startsOffline() {
        DriverProfile d = service.register("u1", new DriverRequest("L1", "Colombo"));
        assertEquals("u1", d.getId());
        assertEquals(DriverStatus.OFFLINE, d.getStatus());
    }

    @Test
    void goAvailable_withoutVehicle_rejected() {
        DriverProfile d = new DriverProfile();
        d.setId("u1");
        when(repo.findById("u1")).thenReturn(Optional.of(d));
        assertThrows(ApiException.class,
                () -> service.updateAvailability("u1", new AvailabilityRequest(DriverStatus.AVAILABLE)));
    }

    @Test
    void getById_missing_notFound() {
        when(repo.findById("x")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.getById("x"));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void eligible_sortedNearestFirst_andFiltersRadiusAndStaleLocation() {
        Instant fresh = Instant.now();
        DriverProfile near = driver("near", 6.9275, 79.8620, VehicleType.CAR, fresh);
        DriverProfile mid = driver("mid", 6.9400, 79.8620, VehicleType.CAR, fresh);
        DriverProfile far = driver("far", 7.2906, 80.6337, VehicleType.CAR, fresh);
        DriverProfile stale = driver("stale", 6.9271, 79.8612, VehicleType.CAR, fresh.minus(Duration.ofHours(2)));
        when(repo.findByStatus(DriverStatus.AVAILABLE)).thenReturn(List.of(far, mid, stale, near));

        List<EligibleDriverResponse> result = service.findEligible(6.9271, 79.8612, null, null, null, null);

        assertEquals(List.of("near", "mid"), result.stream().map(EligibleDriverResponse::driverId).toList());
    }

    @Test
    void eligible_vehicleTypeFilter() {
        Instant fresh = Instant.now();
        when(repo.findByStatus(DriverStatus.AVAILABLE)).thenReturn(List.of(
                driver("car", 6.9275, 79.8620, VehicleType.CAR, fresh),
                driver("tuk", 6.9275, 79.8620, VehicleType.TUK, fresh)));
        List<EligibleDriverResponse> result = service.findEligible(6.9271, 79.8612, 5.0, VehicleType.TUK, null, null);
        assertEquals(1, result.size());
        assertEquals("tuk", result.get(0).driverId());
    }

    @Test
    void eligible_invalidCoordinates_badRequest() {
        assertThrows(ApiException.class, () -> service.findEligible(200, 0, null, null, null, null));
    }

    @Test
    void eligible_noDrivers_returnsEmptyList() {
        when(repo.findByStatus(DriverStatus.AVAILABLE)).thenReturn(List.of());
        assertTrue(service.findEligible(6.9, 79.8, null, null, null, null).isEmpty());
    }

    @Test
    void distance_samePoint_isZero() {
        assertEquals(0.0, DriverService.distanceKm(6.9, 79.8, 6.9, 79.8), 0.0001);
    }
}
