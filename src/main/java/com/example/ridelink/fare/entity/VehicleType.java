package com.example.ridelink.fare.entity;

import java.math.BigDecimal;

public enum VehicleType {
    BIKE("0.6"), TUK("0.8"), CAR("1.0"), VAN("1.4");

    private final BigDecimal multiplier;

    VehicleType(String multiplier) {
        this.multiplier = new BigDecimal(multiplier);
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }
}
