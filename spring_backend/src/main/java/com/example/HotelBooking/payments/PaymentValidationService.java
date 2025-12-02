package com.example.HotelBooking.payments;

import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.PaymentLink;
import com.example.HotelBooking.enums.PaymentStatus;
import com.example.HotelBooking.exceptions.InvalidBookingStateAndDateException;
import com.example.HotelBooking.exceptions.NotFoundException;
import com.example.HotelBooking.repositories.PaymentLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentValidationService {

    private final PaymentLinkRepository paymentLinkRepository;

    /**
     * Validate payment token và trả về booking
     * Throws exception nếu token invalid/expired/already used
     */
    @Transactional(readOnly = true)
    public void validatePaymentToken(String token) {
        PaymentLink link = paymentLinkRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Invalid payment token"));

        // Check expired
        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingStateAndDateException("Payment link has expired");
        }

        Booking booking = link.getBooking();

        // Check already paid
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidBookingStateAndDateException("This booking has already been paid");
        }

        // Check cancelled
        if (booking.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new InvalidBookingStateAndDateException("This booking has been cancelled");
        }
    }
}