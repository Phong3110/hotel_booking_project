package com.example.HotelBooking.services;

import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.enums.PaymentStatus;
import com.example.HotelBooking.repositories.BookingRepository;
import com.example.HotelBooking.repositories.PaymentLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTasks {

    private final PaymentLinkRepository paymentLinkRepository;
    private final BookingRepository bookingRepository;
    private final RoomAvailabilityService roomAvailabilityService;

    /**
     * THÊM: Auto-cancel bookings với payment pending quá lâu (30 phút)
     */
    @Scheduled(cron = "0 */10 * * * *") // Every 10 minutes
    @Transactional
    public void cancelExpiredPendingBookings() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

            List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(
                    PaymentStatus.PENDING,
                    cutoff
            );

            for (Booking booking : expiredBookings) {
                // Release room dates TRƯỚC khi update status
                roomAvailabilityService.releaseRoomDates(
                        booking.getRoom(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate()
                );

                // Update statuses
                booking.setPaymentStatus(PaymentStatus.CANCELLED);
                booking.setBookingStatus(com.example.HotelBooking.enums.BookingStatus.CANCELLED);
            }

            if (!expiredBookings.isEmpty()) {
                bookingRepository.saveAll(expiredBookings);
                log.info("Auto-cancelled {} expired pending bookings", expiredBookings.size());
            }
        } catch (Exception e) {
            log.error("Error auto-cancelling expired bookings", e);
        }
    }
}
