package com.example.HotelBooking.services.impl;

import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.Room;
import com.example.HotelBooking.entities.RoomAvailability;
import com.example.HotelBooking.exceptions.InvalidBookingStateAndDateException;
import com.example.HotelBooking.repositories.RoomAvailabilityRepository;
import com.example.HotelBooking.services.RoomAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {
    private final RoomAvailabilityRepository repo;

    @Override
    public boolean isAvailable(Long roomId, LocalDate start, LocalDate end) {
        return repo.isRoomAvailable(roomId, start, end);
    }

    @Override
    @Transactional
    public void bookRoomDates(Room room, LocalDate start, LocalDate end, Booking booking) {
        // Query 1 lần cho toàn bộ range thay vì loop
        List<RoomAvailability> existing = repo.findByRoomIdAndDateBetween(
                room.getId(),
                start,
                end.minusDays(1) // end is exclusive
        );

        // Tạo map để tra cứu nhanh O(1)
        Map<LocalDate, RoomAvailability> existingMap = existing.stream()
                .collect(Collectors.toMap(RoomAvailability::getDate, Function.identity()));

        // iterate days [start, end) - typical hotel nights counting
        List<RoomAvailability> toSave = new ArrayList<>();
        LocalDate current = start;

        while (current.isBefore(end)) {
            RoomAvailability availability = existingMap.get(current);

            if (availability != null) {
                // Row đã tồn tại
                if (availability.isBooked()) {
                    throw new InvalidBookingStateAndDateException(
                            "Room already booked for date: " + current
                    );
                }
                availability.setBooked(true);
                availability.setBooking(booking);
                toSave.add(availability);
            } else {
                // Tạo row mới
                RoomAvailability newAvailability = RoomAvailability.builder()
                        .room(room)
                        .date(current)
                        .booked(true)
                        .booking(booking)
                        .build();
                toSave.add(newAvailability);
            }

            current = current.plusDays(1);
        }

        // Batch save - 1 query duy nhất
        repo.saveAll(toSave);
    }

    @Override
    @Transactional
    public void releaseRoomDates(Room room, LocalDate start, LocalDate end) {
        // mark existing booked dates in range as available
        repo.markRangeAvailable(room.getId(), start, end.minusDays(1));
        // optional: cleanup orphan unbooked rows to keep table small
        repo.deleteUnbookedRange(room.getId(), start, end.minusDays(1));
    }
}
