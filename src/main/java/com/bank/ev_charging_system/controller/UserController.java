package com.bank.ev_charging_system.controller;

import com.bank.ev_charging_system.dto.*;
import com.bank.ev_charging_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getCurrentUserProfile(), "Profile fetched."));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone) {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        UserProfileResponse updated =
                userService.updateProfile(profile.getId(), name, phone);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Profile updated."));
    }
}