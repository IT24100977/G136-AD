package com.example.ridelink.fare.controller;

import com.example.ridelink.fare.dto.PaymentRequest;
import com.example.ridelink.fare.dto.PaymentResponse;
import com.example.ridelink.fare.dto.ReceiptResponse;
import com.example.ridelink.fare.security.Actor;
import com.example.ridelink.fare.service.PaymentService;
import com.example.ridelink.fare.service.RecordedPayment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /** 201 for a new payment, 200 if the ride already had one. A declined payment is still a stored record (status FAILED). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> record(@Valid @RequestBody PaymentRequest request) {
        RecordedPayment result = service.record(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(PaymentResponse.from(result.payment()));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> retry(@PathVariable String id) {
        return ResponseEntity.ok(PaymentResponse.from(service.retry(id)));
    }

    @GetMapping("/ride/{rideId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> byRide(Authentication auth, @PathVariable String rideId) {
        return ResponseEntity.ok(PaymentResponse.from(service.getByRide(rideId, Actor.from(auth))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> get(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(PaymentResponse.from(service.get(id, Actor.from(auth))));
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceiptResponse> receipt(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(service.receipt(id, Actor.from(auth)));
    }
}
