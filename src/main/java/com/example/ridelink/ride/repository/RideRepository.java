package com.example.ridelink.ride.repository;

import com.example.ridelink.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, String> {
    List<Ride> findByPassengerIdOrderByRequestedAtDesc(String passengerId);
    List<Ride> findByDriverIdOrderByRequestedAtDesc(String driverId);
}
