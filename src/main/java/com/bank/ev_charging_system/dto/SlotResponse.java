package com.bank.ev_charging_system.dto;

import com.bank.ev_charging_system.entity.ChargingSlot;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {

    private Long id;
    private String slotNumber;
    private ChargingSlot.ConnectorType connectorType;
    private BigDecimal powerKw;
    private ChargingSlot.SlotStatus status;
    private BigDecimal pricePerHour;
    private boolean active;
    private Long stationId;
    private String stationName;
}