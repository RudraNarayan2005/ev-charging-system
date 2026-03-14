package com.bank.ev_charging_system.dto;

import com.bank.ev_charging_system.entity.ChargingSlot;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAvailabilityResponse {

    private Long slotId;
    private String slotNumber;
    private ChargingSlot.ConnectorType connectorType;
    private BigDecimal powerKw;
    private BigDecimal pricePerHour;
    private boolean available;
    private LocalDateTime requestedStartTime;
    private LocalDateTime requestedEndTime;
    private BigDecimal estimatedCost;
}