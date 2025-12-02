package com.example.HotelBooking.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "room_availability",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"room_id", "date"})},
        indexes = {@Index(name = "idx_room_date", columnList = "room_id, date")})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "is_booked", nullable = false)
    private boolean booked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
