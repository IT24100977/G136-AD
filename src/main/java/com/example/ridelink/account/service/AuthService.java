package com.example.ridelink.account.service;

import com.example.ridelink.account.dto.LoginRequest;
import com.example.ridelink.account.dto.LoginResponse;
import com.example.ridelink.account.dto.RegisterRequest;
import com.example.ridelink.account.entity.AccountStatus;
import com.example.ridelink.account.entity.User;
import com.example.ridelink.account.exception.DuplicateEmailException;
import com.example.ridelink.account.exception.InvalidCredentialsException;
import com.example.ridelink.account.repository.UserRepository;
import com.example.ridelink.account.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getRole().name());
    }
}
