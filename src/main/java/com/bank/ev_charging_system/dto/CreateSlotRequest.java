package com.bank.ev_charging_system.dto;

import com.bank.ev_charging_system.entity.ChargingSlot;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSlotRequest {

    @NotBlank(message = "Slot number is required")
    private String slotNumber;

    @NotNull(message = "Connector type is required")
    private ChargingSlot.ConnectorType connectorType;

    @NotNull(message = "Power kW is required")
    @DecimalMin(value = "1.0", message = "Minimum power is 1 kW")
    private BigDecimal powerKw;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.1", message = "Price must be greater than 0")
    private BigDecimal pricePerHour;
}