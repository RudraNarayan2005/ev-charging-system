package com.bank.ev_charging_system.dto;

import com.bank.ev_charging_system.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private User.Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private long upcomingBookings;
}