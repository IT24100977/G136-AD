package com.example.ridelink.fare.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** One simulated payment per ride. passengerId / driverId / rideId are references only. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String rideId;
    @Column(nullable = false) private String passengerId;
    @Column(nullable = false) private String driverId;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String method = "SIMULATED_CARD";
    private String failureReason;
    private String receiptNumber;
    private Instant createdAt = Instant.now();
    private Instant paidAt;

    public Payment() { }

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getRideId() { return rideId; }
    public void setRideId(String v) { this.rideId = v; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String v) { this.passengerId = v; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String v) { this.driverId = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus v) { this.status = v; }
    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { this.failureReason = v; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String v) { this.receiptNumber = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant v) { this.paidAt = v; }
}
