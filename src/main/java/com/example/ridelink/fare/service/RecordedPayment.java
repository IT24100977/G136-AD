package com.example.ridelink.fare.service;

import com.example.ridelink.fare.entity.Payment;

/** created = false means an existing payment for the ride was returned. */
public record RecordedPayment(Payment payment, boolean created) {
}
