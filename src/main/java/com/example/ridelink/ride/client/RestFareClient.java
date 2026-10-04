package com.example.ridelink.ride.client;

import com.example.ridelink.ride.security.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ASSUMED endpoints (change to match the Fare & Payment Service):
 *   POST /fares/estimate, POST /fares/final, POST /payments
 */
@Component
public class RestFareClient extends RemoteCallSupport implements FareClient {

    private static final String NAME = "Fare service";

    private final RestClient client;
    private final ServiceTokenProvider tokens;

    public RestFareClient(@Value("${app.services.fare-url}") String baseUrl,
                          @Value("${app.services.connect-timeout-ms}") int connectMs,
                          @Value("${app.services.read-timeout-ms}") int readMs,
                          ServiceTokenProvider tokens) {
        this.client = buildClient(baseUrl, connectMs, readMs);
        this.tokens = tokens;
    }

    @Override
    public FareEstimate estimate(double pickupLat, double pickupLng, double destLat, double destLng, String vehicleType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pickupLatitude", pickupLat);
        body.put("pickupLongitude", pickupLng);
        body.put("destinationLatitude", destLat);
        body.put("destinationLongitude", destLng);
        body.put("vehicleType", vehicleType);
        return post("/fares/estimate", body, FareEstimate.class);
    }

    @Override
    public FinalFare finalFare(String rideId, double distanceKm, long durationMinutes, String vehicleType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rideId", rideId);
        body.put("distanceKm", distanceKm);
        body.put("durationMinutes", durationMinutes);
        body.put("vehicleType", vehicleType);
        return post("/fares/final", body, FinalFare.class);
    }

    @Override
    public PaymentResult recordPayment(String rideId, String passengerId, String driverId, BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rideId", rideId);
        body.put("passengerId", passengerId);
        body.put("driverId", driverId);
        body.put("amount", amount);
        return post("/payments", body, PaymentResult.class);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        return call(NAME, () -> client.post().uri(path)
                .headers(h -> h.setBearerAuth(tokens.token()))
                .body(body)
                .retrieve()
                .body(type));
    }
}
