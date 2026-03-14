package com.bank.ev_charging_system.service;

import com.bank.ev_charging_system.dto.*;
import com.bank.ev_charging_system.entity.*;
import com.bank.ev_charging_system.exception.ResourceNotFoundException;
import com.bank.ev_charging_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingStationService {

    private final ChargingStationRepository stationRepository;
    private final ChargingSlotRepository slotRepository;

    @Transactional(readOnly = true)
    public PagedResponse<StationResponse> getAllActiveStations(
            int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortBy).ascending());
        Page<ChargingStation> result = stationRepository
                .findByStatus(ChargingStation.StationStatus.ACTIVE, pageable);
        return PagedResponse.of(result.map(s -> mapToResponse(s, false)));
    }

    @Transactional(readOnly = true)
    public StationResponse getStationById(Long stationId) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ChargingStation", stationId));
        return mapToResponse(station, true);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StationResponse> searchStations(
            String city, ChargingSlot.ConnectorType connectorType,
            BigDecimal maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChargingStation> result = stationRepository
                .searchStations(city, connectorType, maxPrice, pageable);
        return PagedResponse.of(result.map(s -> mapToResponse(s, false)));
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getNearbyStations(
            double latitude, double longitude, double radiusKm) {
        return stationRepository
                .findNearbyStations(latitude, longitude, radiusKm)
                .stream()
                .map(s -> mapToResponse(s, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public StationResponse createStation(CreateStationRequest request) {
        ChargingStation station = ChargingStation.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .operatingHours(request.getOperatingHours())
                .pricePerKwh(request.getPricePerKwh())
                .amenities(request.getAmenities())
                .description(request.getDescription())
                .status(ChargingStation.StationStatus.ACTIVE)
                .build();
        station = stationRepository.save(station);
        log.info("Station created: {}", station.getName());
        return mapToResponse(station, false);
    }

    @Transactional
    public SlotResponse addSlotToStation(
            Long stationId, CreateSlotRequest request) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ChargingStation", stationId));
        ChargingSlot slot = ChargingSlot.builder()
                .slotNumber(request.getSlotNumber())
                .connectorType(request.getConnectorType())
                .powerKw(request.getPowerKw())
                .pricePerHour(request.getPricePerHour())
                .station(station)
                .status(ChargingSlot.SlotStatus.AVAILABLE)
                .active(true)
                .build();
        slot = slotRepository.save(slot);
        log.info("Slot {} added to station {}",
                slot.getSlotNumber(), stationId);
        return mapSlotToResponse(slot);
    }

    @Transactional(readOnly = true)
    public List<SlotAvailabilityResponse> checkSlotAvailability(
            Long stationId, LocalDateTime startTime, LocalDateTime endTime) {
        return slotRepository
                .findByStationIdAndActiveTrue(stationId)
                .stream().map(slot -> {
                    boolean available = slotRepository
                            .countConflictingBookings(
                                    slot.getId(), startTime, endTime) == 0
                            && slot.getStatus()
                            != ChargingSlot.SlotStatus.UNDER_MAINTENANCE;
                    long minutes = Duration
                            .between(startTime, endTime).toMinutes();
                    BigDecimal cost = slot.getPricePerHour()
                            .multiply(BigDecimal.valueOf(minutes))
                            .divide(BigDecimal.valueOf(60),
                                    2, RoundingMode.HALF_UP);
                    return SlotAvailabilityResponse.builder()
                            .slotId(slot.getId())
                            .slotNumber(slot.getSlotNumber())
                            .connectorType(slot.getConnectorType())
                            .powerKw(slot.getPowerKw())
                            .pricePerHour(slot.getPricePerHour())
                            .available(available)
                            .requestedStartTime(startTime)
                            .requestedEndTime(endTime)
                            .estimatedCost(available ? cost : null)
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional
    public StationResponse updateStationStatus(
            Long stationId, ChargingStation.StationStatus status) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ChargingStation", stationId));
        station.setStatus(status);
        return mapToResponse(stationRepository.save(station), false);
    }

    StationResponse mapToResponse(ChargingStation s, boolean includeSlots) {
        List<ChargingSlot> slots = s.getSlots() == null
                ? List.of() : s.getSlots();
        int total = slots.size();
        int available = (int) slots.stream()
                .filter(sl -> sl.isActive()
                        && sl.getStatus() == ChargingSlot.SlotStatus.AVAILABLE)
                .count();
        return StationResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .address(s.getAddress())
                .city(s.getCity())
                .state(s.getState())
                .pincode(s.getPincode())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .status(s.getStatus())
                .operatingHours(s.getOperatingHours())
                .pricePerKwh(s.getPricePerKwh())
                .amenities(s.getAmenities())
                .description(s.getDescription())
                .totalSlots(total)
                .availableSlots(available)
                .createdAt(s.getCreatedAt())
                .slots(includeSlots ? slots.stream()
                        .filter(ChargingSlot::isActive)
                        .map(this::mapSlotToResponse)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private SlotResponse mapSlotToResponse(ChargingSlot sl) {
        return SlotResponse.builder()
                .id(sl.getId())
                .slotNumber(sl.getSlotNumber())
                .connectorType(sl.getConnectorType())
                .powerKw(sl.getPowerKw())
                .status(sl.getStatus())
                .pricePerHour(sl.getPricePerHour())
                .active(sl.isActive())
                .stationId(sl.getStation().getId())
                .stationName(sl.getStation().getName())
                .build();
    }
}