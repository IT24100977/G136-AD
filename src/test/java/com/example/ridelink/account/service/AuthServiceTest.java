package com.example.ridelink.account.service;

import com.example.ridelink.account.dto.LoginRequest;
import com.example.ridelink.account.dto.LoginResponse;
import com.example.ridelink.account.dto.RegisterRequest;
import com.example.ridelink.account.entity.AccountStatus;
import com.example.ridelink.account.entity.Role;
import com.example.ridelink.account.entity.User;
import com.example.ridelink.account.exception.DuplicateEmailException;
import com.example.ridelink.account.exception.InvalidCredentialsException;
import com.example.ridelink.account.repository.UserRepository;
import com.example.ridelink.account.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesActiveUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Kavisha Perera");
        request.setEmail("kavisha@example.com");
        request.setPassword("password123");
        request.setRole(Role.PASSENGER);

        when(userRepository.existsByEmail("kavisha@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(request);

        assertEquals(AccountStatus.ACTIVE, result.getStatus());
        assertEquals(Role.PASSENGER, result.getRole());
        assertNotEquals("password123", result.getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", result.getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Someone Else");
        request.setEmail("kavisha@example.com");
        request.setPassword("password123");
        request.setRole(Role.PASSENGER);

        when(userRepository.existsByEmail("kavisha@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginFailsWithWrongPassword() {
        User existing = new User();
        existing.setId("user-1");
        existing.setEmail("kavisha@example.com");
        existing.setPasswordHash(passwordEncoder.encode("correct-password"));
        existing.setRole(Role.PASSENGER);
        existing.setStatus(AccountStatus.ACTIVE);

        LoginRequest request = new LoginRequest();
        request.setEmail("kavisha@example.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("kavisha@example.com")).thenReturn(Optional.of(existing));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginSucceedsAndReturnsToken() {
        User existing = new User();
        existing.setId("user-1");
        existing.setEmail("kavisha@example.com");
        existing.setPasswordHash(passwordEncoder.encode("correct-password"));
        existing.setRole(Role.PASSENGER);
        existing.setStatus(AccountStatus.ACTIVE);

        LoginRequest request = new LoginRequest();
        request.setEmail("kavisha@example.com");
        request.setPassword("correct-password");

        when(userRepository.findByEmail("kavisha@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("user-1", response.getUserId());
        assertEquals("PASSENGER", response.getRole());
    }
}
