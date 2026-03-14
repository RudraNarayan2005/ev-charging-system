package com.bank.ev_charging_system.controller;

import com.bank.ev_charging_system.dto.*;
import com.bank.ev_charging_system.entity.ChargingSlot;
import com.bank.ev_charging_system.entity.ChargingStation;
import com.bank.ev_charging_system.service.ChargingStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/stations")
@RequiredArgsConstructor
public class ChargingStationController {

    private final ChargingStationService stationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StationResponse>>>
    getAllStations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(ApiResponse.success(
                stationService.getAllActiveStations(page, size, sortBy),
                "Stations fetched."));
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<ApiResponse<StationResponse>> getStation(
            @PathVariable Long stationId) {
        return ResponseEntity.ok(ApiResponse.success(
                stationService.getStationById(stationId),
                "Station fetched."));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<StationResponse>>>
    searchStations(
            @RequestParam(required = false) String city,
            @RequestParam(required = false)
            ChargingSlot.ConnectorType connectorType,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                stationService.searchStations(
                        city, connectorType, maxPrice, page, size),
                "Search results."));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<StationResponse>>> getNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        return ResponseEntity.ok(ApiResponse.success(
                stationService.getNearbyStations(
                        latitude, longitude, radiusKm),
                "Nearby stations."));
    }

    @GetMapping("/{stationId}/availability")
    public ResponseEntity<ApiResponse<List<SlotAvailabilityResponse>>>
    checkAvailability(
            @PathVariable Long stationId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.success(
                stationService.checkSlotAvailability(
                        stationId, startTime, endTime),
                "Availability checked."));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STATION_OPERATOR')")
    public ResponseEntity<ApiResponse<StationResponse>> createStation(
            @Valid @RequestBody CreateStationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        stationService.createStation(request),
                        "Station created."));
    }

    @PostMapping("/{stationId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN','STATION_OPERATOR')")
    public ResponseEntity<ApiResponse<SlotResponse>> addSlot(
            @PathVariable Long stationId,
            @Valid @RequestBody CreateSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        stationService.addSlotToStation(stationId, request),
                        "Slot added."));
    }

    @PatchMapping("/{stationId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STATION_OPERATOR')")
    public ResponseEntity<ApiResponse<StationResponse>> updateStatus(
            @PathVariable Long stationId,
            @RequestParam ChargingStation.StationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                stationService.updateStationStatus(stationId, status),
                "Status updated."));
    }
}