package com.example.ridelink.driver.repository;

import com.example.ridelink.driver.entity.DriverProfile;
import com.example.ridelink.driver.entity.DriverStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DriverRepository extends MongoRepository<DriverProfile, String> {
    List<DriverProfile> findByStatus(DriverStatus status);
    boolean existsByLicenseNumber(String licenseNumber);
}
