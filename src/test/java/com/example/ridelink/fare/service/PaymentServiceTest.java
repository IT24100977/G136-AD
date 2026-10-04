package com.example.ridelink.fare.service;

import com.example.ridelink.fare.dto.PaymentRequest;
import com.example.ridelink.fare.dto.ReceiptResponse;
import com.example.ridelink.fare.entity.Fare;
import com.example.ridelink.fare.entity.Payment;
import com.example.ridelink.fare.entity.PaymentStatus;
import com.example.ridelink.fare.entity.VehicleType;
import com.example.ridelink.fare.exception.ApiException;
import com.example.ridelink.fare.repository.FareRepository;
import com.example.ridelink.fare.repository.PaymentRepository;
import com.example.ridelink.fare.security.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository payments;
    private FareRepository fares;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        payments = Mockito.mock(PaymentRepository.class);
        fares = Mockito.mock(FareRepository.class);
        service = new PaymentService(payments, fares, new BigDecimal("50000"));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Fare fare(String total) {
        Fare f = new Fare();
        f.setRideId("r1");
        f.setVehicleType(VehicleType.CAR);
        f.setTotal(new BigDecimal(total));
        f.setCurrency("LKR");
        f.setBaseFare(new BigDecimal("100.00"));
        f.setDistanceCharge(new BigDecimal("300.00"));
        f.setTimeCharge(new BigDecimal("75.00"));
        f.setMultiplier(new BigDecimal("1.0"));
        when(fares.findByRideId("r1")).thenReturn(Optional.of(f));
        return f;
    }

    private PaymentRequest request(String amount, Boolean fail) {
        return new PaymentRequest("r1", "p1", "d1", new BigDecimal(amount), fail);
    }

    private Payment storedPayment(PaymentStatus status) {
        Payment p = new Payment();
        p.setId("pay-1");
        p.setRideId("r1");
        p.setPassengerId("p1");
        p.setDriverId("d1");
        p.setAmount(new BigDecimal("475.00"));
        p.setCurrency("LKR");
        p.setStatus(status);
        p.setReceiptNumber(status == PaymentStatus.PAID ? "RCPT-20261003-ABCD1234" : null);
        when(payments.findById("pay-1")).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void record_withoutFinalFare_conflict() {
        when(fares.findByRideId("r1")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.record(request("475.00", null)));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void record_amountMismatch_badRequest() {
        fare("475.00");
        ApiException ex = assertThrows(ApiException.class, () -> service.record(request("100.00", null)));
        assertEquals(400, ex.getStatus().value());
        verify(payments, never()).save(any());
    }

    @Test
    void record_success_isPaidWithReceipt() {
        fare("475.00");
        RecordedPayment r = service.record(request("475.00", null));
        assertTrue(r.created());
        assertEquals(PaymentStatus.PAID, r.payment().getStatus());
        assertTrue(r.payment().getReceiptNumber().startsWith("RCPT-"));
        assertNotNull(r.payment().getPaidAt());
    }

    @Test
    void record_simulatedFailure_isFailedWithoutReceipt() {
        fare("475.00");
        RecordedPayment r = service.record(request("475.00", true));
        assertEquals(PaymentStatus.FAILED, r.payment().getStatus());
        assertNull(r.payment().getReceiptNumber());
        assertNotNull(r.payment().getFailureReason());
    }

    @Test
    void record_amountAboveLimit_isDeclined() {
        fare("60000.00");
        RecordedPayment r = service.record(request("60000.00", null));
        assertEquals(PaymentStatus.FAILED, r.payment().getStatus());
    }

    @Test
    void record_alreadyPaid_returnsExistingWithoutNewPayment() {
        fare("475.00");
        Payment paid = storedPayment(PaymentStatus.PAID);
        when(payments.findByRideId("r1")).thenReturn(Optional.of(paid));

        RecordedPayment r = service.record(request("475.00", null));

        assertFalse(r.created());
        assertSame(paid, r.payment());
        verify(payments, never()).save(any());
    }

    @Test
    void retry_failedPayment_becomesPaid() {
        Payment failed = storedPayment(PaymentStatus.FAILED);
        Payment result = service.retry("pay-1");
        assertEquals(PaymentStatus.PAID, result.getStatus());
        assertSame(failed, result);
    }

    @Test
    void retry_paidPayment_conflict() {
        storedPayment(PaymentStatus.PAID);
        assertThrows(ApiException.class, () -> service.retry("pay-1"));
    }

    @Test
    void receipt_forPaidPayment_includesBreakdown() {
        storedPayment(PaymentStatus.PAID);
        fare("475.00");
        ReceiptResponse receipt = service.receipt("pay-1", new Actor("p1", "PASSENGER"));
        assertEquals("RCPT-20261003-ABCD1234", receipt.receiptNumber());
        assertEquals(0, new BigDecimal("300.00").compareTo(receipt.distanceCharge()));
    }

    @Test
    void receipt_forFailedPayment_conflict() {
        storedPayment(PaymentStatus.FAILED);
        ApiException ex = assertThrows(ApiException.class, () -> service.receipt("pay-1", new Actor("p1", "PASSENGER")));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void get_byStranger_forbidden_participantAndAdminAllowed() {
        storedPayment(PaymentStatus.PAID);
        assertThrows(ApiException.class, () -> service.get("pay-1", new Actor("x", "PASSENGER")));
        assertEquals("pay-1", service.get("pay-1", new Actor("p1", "PASSENGER")).getId());
        assertEquals("pay-1", service.get("pay-1", new Actor("d1", "DRIVER")).getId());
        assertEquals("pay-1", service.get("pay-1", new Actor("a", "ADMIN")).getId());
    }

    @Test
    void get_missing_notFound() {
        when(payments.findById("nope")).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.get("nope", new Actor("a", "ADMIN")));
        assertEquals(404, ex.getStatus().value());
    }
}
