package com.example.ridelink.fare.service;

import com.example.ridelink.fare.dto.EstimateRequest;
import com.example.ridelink.fare.dto.EstimateResponse;
import com.example.ridelink.fare.dto.FinalFareRequest;
import com.example.ridelink.fare.entity.Fare;
import com.example.ridelink.fare.entity.VehicleType;
import com.example.ridelink.fare.exception.ApiException;
import com.example.ridelink.fare.repository.FareRepository;
import com.example.ridelink.fare.util.GeoUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class FareService {

    private final FareRepository fares;
    private final FareCalculator calculator;

    public FareService(FareRepository fares, FareCalculator calculator) {
        this.fares = fares;
        this.calculator = calculator;
    }

    /** Not stored: an estimate is advice, the final fare is the record. */
    public EstimateResponse estimate(EstimateRequest request) {
        VehicleType type = calculator.parseVehicleType(request.vehicleType());
        double distance = round2(GeoUtils.distanceKm(request.pickupLatitude(), request.pickupLongitude(),
                request.destinationLatitude(), request.destinationLongitude()));
        long minutes = calculator.estimateDurationMinutes(distance);
        FareBreakdown b = calculator.calculate(distance, minutes, type);
        return new EstimateResponse(b.total(), calculator.currency(), distance, minutes, type.name(),
                b.baseFare(), b.distanceCharge(), b.timeCharge(), b.multiplier());
    }

    /** Idempotent per ride: asking again for the same ride returns the stored fare. */
    public Fare finalFare(FinalFareRequest request) {
        return fares.findByRideId(request.rideId()).orElseGet(() -> {
            VehicleType type = calculator.parseVehicleType(request.vehicleType());
            FareBreakdown b = calculator.calculate(request.distanceKm(), request.durationMinutes(), type);
            Fare fare = new Fare();
            fare.setId(UUID.randomUUID().toString());
            fare.setRideId(request.rideId());
            fare.setVehicleType(type);
            fare.setDistanceKm(request.distanceKm());
            fare.setDurationMinutes(request.durationMinutes());
            fare.setBaseFare(b.baseFare());
            fare.setDistanceCharge(b.distanceCharge());
            fare.setTimeCharge(b.timeCharge());
            fare.setMultiplier(b.multiplier());
            fare.setTotal(b.total());
            fare.setCurrency(calculator.currency());
            fare.setCreatedAt(Instant.now());
            return fares.save(fare);
        });
    }

    public Fare getByRide(String rideId) {
        return fares.findByRideId(rideId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No final fare for ride: " + rideId));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
