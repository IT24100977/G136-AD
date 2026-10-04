package com.example.ridelink.fare.security;

import org.springframework.security.core.Authentication;

/** The authenticated caller: account id (JWT sub) and role (PASSENGER, DRIVER, ADMIN). */
public record Actor(String id, String role) {

    public static Actor from(Authentication auth) {
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse("");
        return new Actor(auth.getName(), role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
