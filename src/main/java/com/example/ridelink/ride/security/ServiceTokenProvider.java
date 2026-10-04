package com.example.ridelink.ride.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Mints a short-lived service-to-service token (role ADMIN, subject "ride-service") signed with the
 * shared secret. Used when this service changes a driver's availability on the passenger's/driver's behalf.
 */
@Component
public class ServiceTokenProvider {

    private final SecretKey key;

    public ServiceTokenProvider(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String token() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("ride-service")
                .claim("role", "ADMIN")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 5 * 60 * 1000))
                .signWith(key)
                .compact();
    }
}
