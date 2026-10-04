package com.example.ridelink.account.controller;

import com.example.ridelink.account.dto.StatusUpdateRequest;
import com.example.ridelink.account.dto.UpdateProfileRequest;
import com.example.ridelink.account.dto.UserProfileResponse;
import com.example.ridelink.account.entity.User;
import com.example.ridelink.account.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable String id,
                                                               @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(id, request);
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateStatus(@PathVariable String id,
                                                              @Valid @RequestBody StatusUpdateRequest request) {
        User user = userService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }
}
