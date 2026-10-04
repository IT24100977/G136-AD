package com.example.ridelink.ride.repository;

import com.example.ridelink.ride.entity.RideStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideStatusEventRepository extends JpaRepository<RideStatusEvent, Long> {
    List<RideStatusEvent> findByRideIdOrderByIdAsc(String rideId);
}
