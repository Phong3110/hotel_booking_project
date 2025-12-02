package com.example.HotelBooking.repositories;

import com.example.HotelBooking.entities.RoomAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, Long> {
    List<RoomAvailability> findByRoomIdAndDateBetween(Long roomId, LocalDate start, LocalDate end);

    @Modifying
    @Query("DELETE FROM RoomAvailability r WHERE r.room.id = :roomId AND r.date BETWEEN :start AND :end AND r.booked = false")
    void deleteUnbookedRange(@Param("roomId") Long roomId,
                             @Param("start") LocalDate start,
                             @Param("end") LocalDate end);

    // Optional: bulk unset booking (mark available) for a range
    @Modifying
    @Query("UPDATE RoomAvailability r SET r.booked = false, r.booking = null WHERE r.room.id = :roomId AND r.date BETWEEN :start AND :end AND r.booked = true")
    int markRangeAvailable(@Param("roomId") Long roomId,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);

    @Query("""
                SELECT CASE WHEN COUNT(b) = 0 THEN true ELSE false END
                FROM Booking b
                WHERE b.room.id = :roomId
                    AND :checkInDate < b.checkOutDate
                    AND :checkOutDate > b.checkInDate
                    AND b.bookingStatus IN ('BOOKED', 'CHECKED_IN')
            """)
    boolean isRoomAvailable(@Param("roomId") Long roomId,
                            @Param("checkInDate") LocalDate checkInDate,
                            @Param("checkOutDate") LocalDate checkOutDate);
}
