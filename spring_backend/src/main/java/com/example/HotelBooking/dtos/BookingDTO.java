package com.example.HotelBooking.dtos;


import com.example.HotelBooking.enums.BookingStatus;
import com.example.HotelBooking.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDTO {

    private Long id;

    private String bookingReference;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private LocalDateTime createdAt;

    private BookingStatus bookingStatus;

    private PaymentStatus paymentStatus;

    // THÊM: Giá phòng tại thời điểm booking
    private BigDecimal pricePerNightAtBooking;

    private BigDecimal totalPrice;

    private RoomDTO room;

    private UserDTO user;

    // Thêm vào BookingDTO class
    private List<GuestDTO> guests;
}
