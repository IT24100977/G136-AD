package com.example.ridelink.fare.repository;

import com.example.ridelink.fare.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByRideId(String rideId);
}
