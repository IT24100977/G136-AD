package com.example.ridelink.ride.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** passengerId and driverId are Account Service ids (references only, no cross-service FK). */
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String passengerId;
    private String driverId;

    @Column(nullable = false) private String pickupName;
    @Column(nullable = false) private double pickupLatitude;
    @Column(nullable = false) private double pickupLongitude;
    @Column(nullable = false) private String destinationName;
    @Column(nullable = false) private double destinationLatitude;
    @Column(nullable = false) private double destinationLongitude;
    private String vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    private BigDecimal estimatedFare;
    private BigDecimal finalFare;
    private Double distanceKm;

    private String paymentId;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_APPLICABLE;

    private Instant requestedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancelReason;

    public Ride() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String v) { this.passengerId = v; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String v) { this.driverId = v; }
    public String getPickupName() { return pickupName; }
    public void setPickupName(String v) { this.pickupName = v; }
    public double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(double v) { this.pickupLatitude = v; }
    public double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(double v) { this.pickupLongitude = v; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String v) { this.destinationName = v; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(double v) { this.destinationLatitude = v; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(double v) { this.destinationLongitude = v; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String v) { this.vehicleType = v; }
    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus v) { this.status = v; }
    public BigDecimal getEstimatedFare() { return estimatedFare; }
    public void setEstimatedFare(BigDecimal v) { this.estimatedFare = v; }
    public BigDecimal getFinalFare() { return finalFare; }
    public void setFinalFare(BigDecimal v) { this.finalFare = v; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double v) { this.distanceKm = v; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String v) { this.paymentId = v; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus v) { this.paymentStatus = v; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant v) { this.requestedAt = v; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant v) { this.acceptedAt = v; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant v) { this.startedAt = v; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant v) { this.completedAt = v; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant v) { this.cancelledAt = v; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String v) { this.cancelReason = v; }
}
