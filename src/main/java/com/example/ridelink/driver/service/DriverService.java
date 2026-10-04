package com.example.ridelink.driver.service;

import com.example.ridelink.driver.dto.AvailabilityRequest;
import com.example.ridelink.driver.dto.DriverRequest;
import com.example.ridelink.driver.dto.EligibleDriverResponse;
import com.example.ridelink.driver.dto.LocationRequest;
import com.example.ridelink.driver.dto.VehicleRequest;
import com.example.ridelink.driver.entity.DriverProfile;
import com.example.ridelink.driver.entity.DriverStatus;
import com.example.ridelink.driver.entity.VehicleDetails;
import com.example.ridelink.driver.entity.VehicleType;
import com.example.ridelink.driver.exception.ApiException;
import com.example.ridelink.driver.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class DriverService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DriverRepository repository;
    private final double defaultRadiusKm;
    private final long maxLocationAgeMinutes;

    public DriverService(DriverRepository repository,
                         @Value("${app.driver.default-radius-km:5}") double defaultRadiusKm,
                         @Value("${app.driver.max-location-age-minutes:30}") long maxLocationAgeMinutes) {
        this.repository = repository;
        this.defaultRadiusKm = defaultRadiusKm;
        this.maxLocationAgeMinutes = maxLocationAgeMinutes;
    }

    public DriverProfile register(String accountId, DriverRequest request) {
        if (repository.existsById(accountId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Driver profile already exists for this account");
        }
        if (repository.existsByLicenseNumber(request.licenseNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Licence number already registered");
        }
        DriverProfile d = new DriverProfile();
        d.setId(accountId);
        d.setLicenseNumber(request.licenseNumber());
        d.setServiceArea(request.serviceArea());
        return repository.save(d);
    }

    public DriverProfile getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found: " + id));
    }

    public DriverProfile updateProfile(String id, DriverRequest request) {
        DriverProfile d = getById(id);
        d.setLicenseNumber(request.licenseNumber());
        d.setServiceArea(request.serviceArea());
        return repository.save(d);
    }

    public DriverProfile saveVehicle(String id, VehicleRequest request) {
        DriverProfile d = getById(id);
        VehicleDetails v = new VehicleDetails();
        v.setPlateNumber(request.plateNumber());
        v.setMake(request.make());
        v.setModel(request.model());
        v.setColor(request.color());
        v.setSeats(request.seats());
        v.setType(request.type());
        d.setVehicle(v);
        return repository.save(d);
    }

    public DriverProfile updateAvailability(String id, AvailabilityRequest request) {
        DriverProfile d = getById(id);
        if (request.status() == DriverStatus.AVAILABLE) {
            if (!d.hasVehicle()) {
                throw new ApiException(HttpStatus.CONFLICT, "Register a vehicle before going AVAILABLE");
            }
            if (d.getLatitude() == null) {
                throw new ApiException(HttpStatus.CONFLICT, "Set a current location before going AVAILABLE");
            }
        }
        d.setStatus(request.status());
        return repository.save(d);
    }

    public DriverProfile updateLocation(String id, LocationRequest request) {
        DriverProfile d = getById(id);
        d.setLatitude(request.latitude());
        d.setLongitude(request.longitude());
        d.setLocationUpdatedAt(Instant.now());
        return repository.save(d);
    }

    /**
     * Eligible = AVAILABLE, has a vehicle, has a fresh location, within radius of the pickup,
     * (optionally) matching vehicle type / service area. Sorted nearest first.
     */
    public List<EligibleDriverResponse> findEligible(double lat, double lng, Double radiusKm,
                                                     VehicleType type, String serviceArea, Integer limit) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid pickup coordinates");
        }
        double radius = radiusKm == null ? defaultRadiusKm : radiusKm;
        if (radius <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "radiusKm must be positive");
        }
        int max = (limit == null || limit < 1) ? 5 : Math.min(limit, 20);
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(maxLocationAgeMinutes));

        return repository.findByStatus(DriverStatus.AVAILABLE).stream()
                .filter(DriverProfile::hasVehicle)
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .filter(d -> d.getLocationUpdatedAt() != null && d.getLocationUpdatedAt().isAfter(cutoff))
                .filter(d -> type == null || d.getVehicle().getType() == type)
                .filter(d -> serviceArea == null || serviceArea.isBlank()
                        || d.getServiceArea().equalsIgnoreCase(serviceArea))
                .map(d -> EligibleDriverResponse.from(d, distanceKm(lat, lng, d.getLatitude(), d.getLongitude())))
                .filter(r -> r.distanceKm() <= radius)
                .sorted(Comparator.comparingDouble(EligibleDriverResponse::distanceKm))
                .limit(max)
                .toList();
    }

    /** Haversine great-circle distance in kilometres. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
