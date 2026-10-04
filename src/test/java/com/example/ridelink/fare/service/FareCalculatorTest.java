package com.example.ridelink.fare.service;

import com.example.ridelink.fare.entity.VehicleType;
import com.example.ridelink.fare.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FareCalculatorTest {

    private final FareCalculator calc = new FareCalculator(new BigDecimal("100"), new BigDecimal("60"),
            new BigDecimal("5"), new BigDecimal("250"), 30.0, "LKR");

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void car_workedExample() {
        // 100 + 60 x 5 km + 5 x 15 min = 475.00
        FareBreakdown b = calc.calculate(5.0, 15, VehicleType.CAR);
        assertMoney("300.00", b.distanceCharge());
        assertMoney("75.00", b.timeCharge());
        assertMoney("475.00", b.total());
    }

    @Test
    void vehicleMultipliers_applied() {
        assertMoney("285.00", calc.calculate(5.0, 15, VehicleType.BIKE).total());
        assertMoney("380.00", calc.calculate(5.0, 15, VehicleType.TUK).total());
        assertMoney("665.00", calc.calculate(5.0, 15, VehicleType.VAN).total());
    }

    @Test
    void shortRide_getsMinimumFare() {
        // 100 + 30 + 10 = 140, below the minimum of 250
        assertMoney("250.00", calc.calculate(0.5, 2, VehicleType.CAR).total());
    }

    @Test
    void zeroDistance_stillMinimumFare() {
        assertMoney("250.00", calc.calculate(0.0, 1, VehicleType.CAR).total());
    }

    @Test
    void negativeInput_rejected() {
        assertThrows(ApiException.class, () -> calc.calculate(-1.0, 5, VehicleType.CAR));
        assertThrows(ApiException.class, () -> calc.calculate(1.0, -5, VehicleType.CAR));
    }

    @Test
    void vehicleType_defaultsToCar_andRejectsUnknown() {
        assertEquals(VehicleType.CAR, calc.parseVehicleType(null));
        assertEquals(VehicleType.CAR, calc.parseVehicleType("  "));
        assertEquals(VehicleType.TUK, calc.parseVehicleType("tuk"));
        assertThrows(ApiException.class, () -> calc.parseVehicleType("spaceship"));
    }

    @Test
    void estimatedDuration_isAtLeastOneMinute() {
        assertEquals(1, calc.estimateDurationMinutes(0.0));
        assertEquals(20, calc.estimateDurationMinutes(10.0)); // 10 km at 30 km/h
    }
}
