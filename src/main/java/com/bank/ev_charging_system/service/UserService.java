package com.bank.ev_charging_system.service;

import com.bank.ev_charging_system.dto.UserProfileResponse;
import com.bank.ev_charging_system.entity.User;
import com.bank.ev_charging_system.exception.ResourceNotFoundException;
import com.bank.ev_charging_system.repository.BookingRepository;
import com.bank.ev_charging_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", userId));
        long upcoming = bookingRepository
                .countUpcomingBookings(userId, LocalDateTime.now());
        return mapToProfile(user, upcoming);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        long upcoming = bookingRepository
                .countUpcomingBookings(user.getId(), LocalDateTime.now());
        return mapToProfile(user, upcoming);
    }

    @Transactional
    public UserProfileResponse updateProfile(
            Long userId, String name, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", userId));
        if (name != null && !name.isBlank()) user.setName(name);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        user = userRepository.save(user);
        long upcoming = bookingRepository
                .countUpcomingBookings(userId, LocalDateTime.now());
        return mapToProfile(user, upcoming);
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Current user not found"));
    }

    private UserProfileResponse mapToProfile(User user, long upcoming) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .upcomingBookings(upcoming)
                .build();
    }
}