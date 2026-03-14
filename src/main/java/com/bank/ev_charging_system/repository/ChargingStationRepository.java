package com.bank.ev_charging_system.repository;

import com.bank.ev_charging_system.entity.ChargingSlot;
import com.bank.ev_charging_system.entity.ChargingStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {

    Page<ChargingStation> findByStatus(
            ChargingStation.StationStatus status, Pageable pageable);

    Page<ChargingStation> findByCityIgnoreCaseAndStatus(
            String city, ChargingStation.StationStatus status, Pageable pageable);

    // Haversine formula — finds stations within radiusKm
    @Query(value = """
            SELECT s.* FROM charging_stations s
            WHERE s.status = 'ACTIVE'
            AND (6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                COS(RADIANS(s.longitude) - RADIANS(:lng)) +
                SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
            )) <= :radiusKm
            ORDER BY (6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                COS(RADIANS(s.longitude) - RADIANS(:lng)) +
                SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
            )) ASC
            """, nativeQuery = true)
    List<ChargingStation> findNearbyStations(
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            @Param("radiusKm") double radiusKm);

    // Search by city, connector type, max price
    @Query("""
            SELECT DISTINCT s FROM ChargingStation s
            JOIN s.slots sl
            WHERE s.status = 'ACTIVE'
            AND sl.active = true
            AND sl.status = 'AVAILABLE'
            AND (:city IS NULL OR LOWER(s.city) = LOWER(:city))
            AND (:connectorType IS NULL OR sl.connectorType = :connectorType)
            AND (:maxPrice IS NULL OR sl.pricePerHour <= :maxPrice)
            """)
    Page<ChargingStation> searchStations(
            @Param("city") String city,
            @Param("connectorType") ChargingSlot.ConnectorType connectorType,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
