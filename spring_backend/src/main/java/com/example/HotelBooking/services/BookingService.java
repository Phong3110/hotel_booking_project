package com.example.HotelBooking.services;

import com.example.HotelBooking.dtos.BookingDTO;
import com.example.HotelBooking.dtos.Response;
import com.example.HotelBooking.dtos.BookingStatusResponse;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

public interface BookingService {

    Response getAllBookings();
    Response createBooking(BookingDTO bookingDTO);
    Response findBookingByReference(String  bookingReference);
    Response updateBooking(BookingDTO bookingDTO);
    BookingStatusResponse checkBookingStatus(String token);
    Response cancelBooking(String bookingReference);
}
