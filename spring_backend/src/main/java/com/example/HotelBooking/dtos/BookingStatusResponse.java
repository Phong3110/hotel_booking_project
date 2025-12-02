package com.example.HotelBooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingStatusResponse {
    private String status;
    private String message;
    private BigDecimal amount;
    private String bookingReference;
}
