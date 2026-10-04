package com.example.ridelink.fare.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** The final fare of one ride. rideId is a reference to the Ride Management Service, not a foreign key. */
@Entity
@Table(name = "fares")
public class Fare {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String rideId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false) private double distanceKm;
    @Column(nullable = false) private long durationMinutes;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal baseFare;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal distanceCharge;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal timeCharge;
    @Column(nullable = false, precision = 6, scale = 2) private BigDecimal multiplier;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(nullable = false) private String currency;
    private Instant createdAt = Instant.now();

    public Fare() { }

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getRideId() { return rideId; }
    public void setRideId(String v) { this.rideId = v; }
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType v) { this.vehicleType = v; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double v) { this.distanceKm = v; }
    public long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(long v) { this.durationMinutes = v; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal v) { this.baseFare = v; }
    public BigDecimal getDistanceCharge() { return distanceCharge; }
    public void setDistanceCharge(BigDecimal v) { this.distanceCharge = v; }
    public BigDecimal getTimeCharge() { return timeCharge; }
    public void setTimeCharge(BigDecimal v) { this.timeCharge = v; }
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal v) { this.multiplier = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { this.total = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
