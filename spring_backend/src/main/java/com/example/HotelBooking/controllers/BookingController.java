package com.example.HotelBooking.controllers;

import com.example.HotelBooking.dtos.BookingDTO;
import com.example.HotelBooking.dtos.BookingStatusResponse;
import com.example.HotelBooking.dtos.Response;
import com.example.HotelBooking.services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllBookings(){
        return ResponseEntity.ok(bookingService.getAllBookings());
    }


    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('CUSTOMER')")
    public ResponseEntity<Response> createBooking(@RequestBody BookingDTO bookingDTO){
        return ResponseEntity.ok(bookingService.createBooking(bookingDTO));
    }


    @GetMapping("/{reference}")
    public ResponseEntity<Response> findBookingByReference(@PathVariable String reference){
        return ResponseEntity.ok(bookingService.findBookingByReference(reference));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> updateBooking(@RequestBody BookingDTO bookingDTO){
        return ResponseEntity.ok(bookingService.updateBooking(bookingDTO));
    }

    @GetMapping("/status")
    public ResponseEntity<BookingStatusResponse> checkBookingStatus(@RequestParam String token) {
        BookingStatusResponse response = bookingService.checkBookingStatus(token);

        if ("OK".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/cancel/{bookingReference}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response> cancelBooking(@PathVariable String bookingReference) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingReference));
    }
}
