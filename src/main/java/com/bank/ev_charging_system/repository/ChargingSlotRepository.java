package com.bank.ev_charging_system.repository;

import com.bank.ev_charging_system.entity.ChargingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChargingSlotRepository extends JpaRepository<ChargingSlot, Long> {

    List<ChargingSlot> findByStationIdAndActiveTrue(Long stationId);

    List<ChargingSlot> findByStationIdAndStatusAndActiveTrue(
            Long stationId, ChargingSlot.SlotStatus status);

    // Find slots NOT booked during the requested time window
    @Query("""
            SELECT sl FROM ChargingSlot sl
            WHERE sl.station.id = :stationId
            AND sl.active = true
            AND sl.status != 'UNDER_MAINTENANCE'
            AND sl.id NOT IN (
                SELECT b.slot.id FROM Booking b
                WHERE b.status IN ('CONFIRMED', 'ACTIVE')
                AND b.startTime < :endTime
                AND b.endTime > :startTime
            )
            """)
    List<ChargingSlot> findAvailableSlots(
            @Param("stationId") Long stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Count conflicting bookings for a slot in a time window
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.slot.id = :slotId
            AND b.status IN ('CONFIRMED', 'ACTIVE')
            AND b.startTime < :endTime
            AND b.endTime > :startTime
            """)
    long countConflictingBookings(
            @Param("slotId") Long slotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}