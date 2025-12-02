package com.example.HotelBooking.repositories;

import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.User;
import com.example.HotelBooking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId); // Fetch all bookings for a specific user


    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByBookingReferenceAndUserEmail(String bookingReference, String email);

    boolean existsByBookingReference(String reference);

    long countByUserAndPaymentStatus(User user, PaymentStatus status);

    // THÊM method này:
    @Query("SELECT b FROM Booking b WHERE b.paymentStatus = :status AND b.createdAt < :cutoffTime")
    List<Booking> findExpiredPendingBookings(
            @Param("status") PaymentStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );
}