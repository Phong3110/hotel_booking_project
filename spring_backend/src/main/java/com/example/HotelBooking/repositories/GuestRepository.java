// spring_backend/src/main/java/com/example/HotelBooking/repositories/GuestRepository.java
package com.example.HotelBooking.repositories;

import com.example.HotelBooking.entities.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}