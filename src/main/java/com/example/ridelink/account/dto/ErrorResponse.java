package com.example.ridelink.account.dto;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private List<String> messages;

    public ErrorResponse(int status, String error, List<String> messages) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.messages = messages;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public List<String> getMessages() {
        return messages;
    }
}
