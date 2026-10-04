package com.example.ridelink.fare.service;

import com.example.ridelink.fare.dto.PaymentRequest;
import com.example.ridelink.fare.dto.ReceiptResponse;
import com.example.ridelink.fare.entity.Fare;
import com.example.ridelink.fare.entity.Payment;
import com.example.ridelink.fare.entity.PaymentStatus;
import com.example.ridelink.fare.exception.ApiException;
import com.example.ridelink.fare.repository.FareRepository;
import com.example.ridelink.fare.repository.PaymentRepository;
import com.example.ridelink.fare.security.Actor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Simulated payments. A payment is declined when the caller asks for a simulated failure or the amount is
 * above the configured limit. Nothing real is charged.
 */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final FareRepository fares;
    private final BigDecimal maxAmount;

    public PaymentService(PaymentRepository payments, FareRepository fares,
                          @Value("${app.payment.max-amount}") BigDecimal maxAmount) {
        this.payments = payments;
        this.fares = fares;
        this.maxAmount = maxAmount;
    }

    public RecordedPayment record(PaymentRequest request) {
        Fare fare = fares.findByRideId(request.rideId()).orElseThrow(() ->
                new ApiException(HttpStatus.CONFLICT, "No final fare has been calculated for ride " + request.rideId()));
        if (fare.getTotal().compareTo(request.amount()) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount does not match the final fare of " + fare.getTotal());
        }

        Payment existing = payments.findByRideId(request.rideId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == PaymentStatus.PAID) {
                return new RecordedPayment(existing, false);
            }
            return new RecordedPayment(process(existing, Boolean.TRUE.equals(request.simulateFailure())), false);
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setRideId(request.rideId());
        payment.setPassengerId(request.passengerId());
        payment.setDriverId(request.driverId());
        payment.setAmount(fare.getTotal());
        payment.setCurrency(fare.getCurrency());
        return new RecordedPayment(process(payment, Boolean.TRUE.equals(request.simulateFailure())), true);
    }

    public Payment retry(String paymentId) {
        Payment payment = load(paymentId);
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only failed payments can be retried");
        }
        return process(payment, false);
    }

    public Payment get(String paymentId, Actor actor) {
        Payment payment = load(paymentId);
        assertParticipantOrAdmin(payment, actor);
        return payment;
    }

    public Payment getByRide(String rideId, Actor actor) {
        Payment payment = payments.findByRideId(rideId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No payment for ride: " + rideId));
        assertParticipantOrAdmin(payment, actor);
        return payment;
    }

    public ReceiptResponse receipt(String paymentId, Actor actor) {
        Payment p = get(paymentId, actor);
        if (p.getStatus() != PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "No receipt: the payment was not successful");
        }
        Fare f = fares.findByRideId(p.getRideId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fare record missing for ride " + p.getRideId()));
        return new ReceiptResponse(p.getReceiptNumber(), p.getId(), p.getRideId(), p.getPassengerId(), p.getDriverId(),
                p.getAmount(), p.getCurrency(), p.getMethod(), p.getPaidAt(), f.getVehicleType().name(),
                f.getDistanceKm(), f.getDurationMinutes(), f.getBaseFare(), f.getDistanceCharge(),
                f.getTimeCharge(), f.getMultiplier());
    }

    // ---- helpers

    private Payment process(Payment payment, boolean forceFailure) {
        if (forceFailure) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined (simulated failure requested)");
        } else if (payment.getAmount().compareTo(maxAmount) > 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined: amount exceeds the simulated limit of " + maxAmount);
        } else {
            Instant now = Instant.now();
            payment.setStatus(PaymentStatus.PAID);
            payment.setFailureReason(null);
            payment.setPaidAt(now);
            payment.setReceiptNumber(receiptNumber(payment.getId(), now));
        }
        return payments.save(payment);
    }

    private static String receiptNumber(String paymentId, Instant when) {
        String date = DateTimeFormatter.BASIC_ISO_DATE.withZone(ZoneOffset.UTC).format(when);
        String suffix = paymentId.replace("-", "");
        suffix = suffix.substring(0, Math.min(8, suffix.length())).toUpperCase();
        return "RCPT-" + date + "-" + suffix;
    }

    private Payment load(String id) {
        return payments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found: " + id));
    }

    private void assertParticipantOrAdmin(Payment p, Actor actor) {
        boolean participant = actor.id().equals(p.getPassengerId()) || actor.id().equals(p.getDriverId());
        if (!participant && !actor.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this payment");
        }
    }
}
