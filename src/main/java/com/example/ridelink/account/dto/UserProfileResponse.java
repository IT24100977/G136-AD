package com.example.ridelink.account.dto;

import com.example.ridelink.account.entity.AccountStatus;
import com.example.ridelink.account.entity.Role;
import com.example.ridelink.account.entity.User;

public class UserProfileResponse {

    private String id;
    private String name;
    private String email;
    private Role role;
    private AccountStatus status;

    public static UserProfileResponse from(User user) {
        UserProfileResponse r = new UserProfileResponse();
        r.id = user.getId();
        r.name = user.getName();
        r.email = user.getEmail();
        r.role = user.getRole();
        r.status = user.getStatus();
        return r;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
