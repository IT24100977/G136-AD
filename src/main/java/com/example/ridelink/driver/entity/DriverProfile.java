package com.example.ridelink.driver.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** The id is the Account Service user id (JWT "sub"); it is a reference, not a foreign key. */
@Document(collection = "driver_profiles")
public class DriverProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String licenseNumber;

    private String serviceArea;

    private DriverStatus status = DriverStatus.OFFLINE;

    private Double latitude;
    private Double longitude;
    private Instant locationUpdatedAt;

    /** Embedded document. */
    private VehicleDetails vehicle;

    private Instant createdAt = Instant.now();

    public DriverProfile() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(Instant t) { this.locationUpdatedAt = t; }
    public VehicleDetails getVehicle() { return vehicle; }
    public void setVehicle(VehicleDetails vehicle) { this.vehicle = vehicle; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean hasVehicle() {
        return vehicle != null && vehicle.getPlateNumber() != null;
    }
}
