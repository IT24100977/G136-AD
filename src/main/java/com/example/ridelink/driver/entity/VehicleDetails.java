package com.example.ridelink.driver.entity;

import org.springframework.data.mongodb.core.index.Indexed;

/** Embedded inside DriverProfile (stored as a nested document). */
public class VehicleDetails {

    /** Sparse unique index: drivers without a vehicle do not collide. */
    @Indexed(unique = true, sparse = true)
    private String plateNumber;
    private String make;
    private String model;
    private String color;
    private Integer seats;
    private VehicleType type;

    public VehicleDetails() {
    }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public VehicleType getType() { return type; }
    public void setType(VehicleType type) { this.type = type; }
}
