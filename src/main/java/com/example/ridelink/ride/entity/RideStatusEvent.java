package com.example.ridelink.ride.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Audit trail of lifecycle changes. */
@Entity
@Table(name = "ride_status_events")
public class RideStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String rideId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private RideStatus status;
    @Column(nullable = false) private Instant changedAt;
    private String changedBy;

    public RideStatusEvent() { }

    public RideStatusEvent(String rideId, RideStatus status, String changedBy) {
        this.rideId = rideId;
        this.status = status;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRideId() { return rideId; }
    public RideStatus getStatus() { return status; }
    public Instant getChangedAt() { return changedAt; }
    public String getChangedBy() { return changedBy; }
}
