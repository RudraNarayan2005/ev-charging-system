package com.bank.ev_charging_system.repository;

import com.bank.ev_charging_system.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(
            Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, Booking.BookingStatus status, Pageable pageable);

    // All bookings for a station between two dates
    @Query("""
            SELECT b FROM Booking b
            WHERE b.slot.station.id = :stationId
            AND b.status IN ('CONFIRMED', 'ACTIVE')
            AND b.startTime >= :from
            AND b.startTime <= :to
            ORDER BY b.startTime ASC
            """)
    List<Booking> findStationBookings(
            @Param("stationId") Long stationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Auto-complete bookings whose end time has passed
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = 'COMPLETED'
            WHERE b.status = 'ACTIVE'
            AND b.endTime < :now
            """)
    int autoCompleteExpiredBookings(@Param("now") LocalDateTime now);

    // Mark no-shows for bookings not started 30 mins after start time
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = 'NO_SHOW'
            WHERE b.status = 'CONFIRMED'
            AND b.startTime < :threshold
            """)
    int markNoShows(@Param("threshold") LocalDateTime threshold);

    // Count upcoming bookings for a user
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.user.id = :userId
            AND b.status = 'CONFIRMED'
            AND b.startTime > :now
            """)
    long countUpcomingBookings(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);
}