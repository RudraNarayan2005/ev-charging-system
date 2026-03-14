package com.bank.ev_charging_system.service;

import com.bank.ev_charging_system.dto.*;
import com.bank.ev_charging_system.entity.*;
import com.bank.ev_charging_system.exception.*;
import com.bank.ev_charging_system.repository.*;
import com.bank.ev_charging_system.util.BookingReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingSlotRepository slotRepository;
    private final UserService userService;
    private final BookingReferenceGenerator referenceGenerator;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createBooking(CreateBookingRequest request) {
        validateTimeWindow(request.getStartTime(), request.getEndTime());

        ChargingSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot", request.getSlotId()));

        if (!slot.isActive()) {
            throw new BadRequestException(
                    "Slot " + slot.getSlotNumber() + " is not active.");
        }
        if (slot.getStatus() == ChargingSlot.SlotStatus.UNDER_MAINTENANCE) {
            throw new BadRequestException(
                    "Slot " + slot.getSlotNumber() + " is under maintenance.");
        }

        long conflicts = slotRepository.countConflictingBookings(
                slot.getId(), request.getStartTime(), request.getEndTime());

        if (conflicts > 0) {
            throw new BookingConflictException(
                    "Slot " + slot.getSlotNumber()
                            + " is already booked for the requested time.");
        }

        long minutes = Duration.between(
                request.getStartTime(), request.getEndTime()).toMinutes();

        BigDecimal totalAmount = slot.getPricePerHour()
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        User currentUser = userService.getCurrentUser();

        Booking booking = Booking.builder()
                .bookingReference(referenceGenerator.generate())
                .user(currentUser)
                .slot(slot)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(totalAmount)
                .vehicleNumber(request.getVehicleNumber())
                .notes(request.getNotes())
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created: {} for user {}",
                booking.getBookingReference(), currentUser.getEmail());

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking", bookingId));

        User currentUser = userService.getCurrentUser();

        if (!booking.getUser().getId().equals(currentUser.getId())
                && currentUser.getRole() != User.Role.ADMIN) {
            throw new BadRequestException(
                    "Not authorized to cancel this booking.");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled.");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot cancel a completed booking.");
        }
        if (booking.getStatus() == Booking.BookingStatus.ACTIVE) {
            throw new BadRequestException(
                    "Cannot cancel an in-progress session.");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
        booking = bookingRepository.save(booking);

        log.info("Booking {} cancelled", booking.getBookingReference());
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getMyBookings(
            Booking.BookingStatus status, int page, int size) {

        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending());

        Page<Booking> result = (status != null)
                ? bookingRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(), status, pageable)
                : bookingRepository
                .findByUserIdOrderByCreatedAtDesc(
                        currentUser.getId(), pageable);

        return PagedResponse.of(result.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking", bookingId));
        User currentUser = userService.getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())
                && currentUser.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Access denied.");
        }
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        Booking booking = bookingRepository
                .findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + reference));
        User currentUser = userService.getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())
                && currentUser.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Access denied.");
        }
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getStationBookings(
            Long stationId, LocalDateTime from, LocalDateTime to) {
        return bookingRepository
                .findStationBookings(stationId, from, to)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void autoCompleteBookings() {
        int count = bookingRepository
                .autoCompleteExpiredBookings(LocalDateTime.now());
        if (count > 0)
            log.info("Auto-completed {} bookings.", count);
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void markNoShows() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        int count = bookingRepository.markNoShows(threshold);
        if (count > 0)
            log.info("Marked {} bookings as NO_SHOW.", count);
    }

    BookingResponse mapToResponse(Booking b) {
        ChargingSlot slot = b.getSlot();
        ChargingStation station = slot.getStation();
        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .userId(b.getUser().getId())
                .userName(b.getUser().getName())
                .slotId(slot.getId())
                .slotNumber(slot.getSlotNumber())
                .stationId(station.getId())
                .stationName(station.getName())
                .stationAddress(station.getAddress())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .totalAmount(b.getTotalAmount())
                .vehicleNumber(b.getVehicleNumber())
                .notes(b.getNotes())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private void validateTimeWindow(
            LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException(
                    "End time must be after start time.");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 15) {
            throw new BadRequestException(
                    "Minimum booking duration is 15 minutes.");
        }
        if (minutes > 24 * 60) {
            throw new BadRequestException(
                    "Maximum booking duration is 24 hours.");
        }
    }
}