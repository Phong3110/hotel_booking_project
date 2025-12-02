package com.example.HotelBooking.services;

import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.Room;

import java.time.LocalDate;

public interface RoomAvailabilityService {
    boolean isAvailable(Long roomId, LocalDate start, LocalDate end);
    void bookRoomDates(Room room, LocalDate start, LocalDate end, Booking booking);
    void releaseRoomDates(Room room, LocalDate start, LocalDate end);
}
