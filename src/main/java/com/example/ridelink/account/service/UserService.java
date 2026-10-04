package com.example.ridelink.account.service;

import com.example.ridelink.account.dto.UpdateProfileRequest;
import com.example.ridelink.account.entity.AccountStatus;
import com.example.ridelink.account.entity.User;
import com.example.ridelink.account.exception.UserNotFoundException;
import com.example.ridelink.account.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User updateProfile(String id, UpdateProfileRequest request) {
        User user = getById(id);
        user.setName(request.getName());
        return userRepository.save(user);
    }

    public User updateStatus(String id, AccountStatus status) {
        User user = getById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }
}
