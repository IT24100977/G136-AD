package com.example.ridelink.account.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String id) {
        super("No account found with id: " + id);
    }
}
