package com.example.ridelink.fare.repository;

import com.example.ridelink.fare.entity.Fare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareRepository extends JpaRepository<Fare, String> {
    Optional<Fare> findByRideId(String rideId);
}
