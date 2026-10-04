package com.example.ridelink.ride.client;

import com.example.ridelink.ride.security.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class RestDriverClient extends RemoteCallSupport implements DriverClient {

    private static final String NAME = "Driver service";

    private final RestClient client;
    private final ServiceTokenProvider tokens;

    public RestDriverClient(@Value("${app.services.driver-url}") String baseUrl,
                            @Value("${app.services.connect-timeout-ms}") int connectMs,
                            @Value("${app.services.read-timeout-ms}") int readMs,
                            ServiceTokenProvider tokens) {
        this.client = buildClient(baseUrl, connectMs, readMs);
        this.tokens = tokens;
    }

    @Override
    public List<EligibleDriver> findEligible(double lat, double lng, String vehicleType) {
        return call(NAME, () -> {
            List<EligibleDriver> result = client.get()
                    .uri(u -> {
                        var b = u.path("/drivers/eligible").queryParam("lat", lat).queryParam("lng", lng)
                                .queryParam("limit", 5);
                        if (vehicleType != null && !vehicleType.isBlank()) {
                            b.queryParam("vehicleType", vehicleType);
                        }
                        return b.build();
                    })
                    .headers(h -> h.setBearerAuth(tokens.token()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EligibleDriver>>() { });
            return result == null ? List.of() : result;
        });
    }

    @Override
    public void updateStatus(String driverId, String status) {
        call(NAME, () -> client.patch()
                .uri("/drivers/{id}/availability", driverId)
                .headers(h -> h.setBearerAuth(tokens.token()))
                .body(Map.of("status", status))
                .retrieve()
                .toBodilessEntity());
    }
}
