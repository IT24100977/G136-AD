package com.example.ridelink.account.controller;

import com.example.ridelink.account.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerThenLoginReturnsValidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String registerBody = """
                {"name":"Test User","email":"test.user@example.com","password":"password123","role":"PASSENGER"}
                """;

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/accounts/register",
                new HttpEntity<>(registerBody, headers),
                String.class);
        assertEquals(201, registerResponse.getStatusCode().value());

        String loginBody = """
                {"email":"test.user@example.com","password":"password123"}
                """;

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/accounts/login",
                new HttpEntity<>(loginBody, headers),
                LoginResponse.class);

        assertEquals(200, loginResponse.getStatusCode().value());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getToken());
        assertEquals("PASSENGER", loginResponse.getBody().getRole());
    }

    @Test
    void registerRejectsShortPassword() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String registerBody = """
                {"name":"Test User 2","email":"test.user2@example.com","password":"short","role":"PASSENGER"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/accounts/register",
                new HttpEntity<>(registerBody, headers),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void protectedProfileEndpointRejectsRequestWithNoToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/accounts/some-random-id", String.class);

        assertEquals(403, response.getStatusCode().value());
    }
}
