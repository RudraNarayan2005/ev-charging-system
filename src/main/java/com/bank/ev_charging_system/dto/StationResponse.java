package com.bank.ev_charging_system.dto;

import com.bank.ev_charging_system.entity.ChargingStation;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private ChargingStation.StationStatus status;
    private String operatingHours;
    private BigDecimal pricePerKwh;
    private String amenities;
    private String description;
    private int totalSlots;
    private int availableSlots;
    private LocalDateTime createdAt;
    private List<SlotResponse> slots;
}