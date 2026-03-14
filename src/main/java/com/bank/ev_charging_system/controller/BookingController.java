package com.bank.ev_charging_system.controller;

import com.bank.ev_charging_system.dto.*;
import com.bank.ev_charging_system.entity.Booking;
import com.bank.ev_charging_system.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking,
                        "Booking confirmed! Ref: "
                                + booking.getBookingReference()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>>
    getMyBookings(
            @RequestParam(required = false) Booking.BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getMyBookings(status, page, size),
                "Bookings fetched."));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingById(bookingId),
                "Booking fetched."));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getByReference(
            @PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingByReference(reference),
                "Booking fetched."));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody CancelBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.cancelBooking(bookingId, request.getReason()),
                "Booking cancelled."));
    }

    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasAnyRole('ADMIN','STATION_OPERATOR')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>>
    getStationBookings(
            @PathVariable Long stationId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getStationBookings(stationId, from, to),
                "Station bookings fetched."));
    }
}