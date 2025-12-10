package com.example.HotelBooking.entities;


import com.example.HotelBooking.enums.BookingStatus;
import com.example.HotelBooking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "bookings")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã đặt phòng (reference) - string random, unique
    @Column(nullable = false, unique = true)
    private String bookingReference;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // THÊM: Lưu giá phòng tại thời điểm booking
    @Column(nullable = false)
    private BigDecimal pricePerNightAtBooking;

    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(cascade = CascadeType.REMOVE)  // meaning when a user is deleted all associated booking of the user will be deleted
    @JoinColumn(name = "user_id")
    private User user;

    // Thêm vào class Booking
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Guest> guests = new ArrayList<>();

    // Thêm helper method
    public void addGuest(Guest guest) {
        guests.add(guest);
        guest.setBooking(this);
    }

    public void removeGuest(Guest guest) {
        guests.remove(guest);
        guest.setBooking(null);
    }
}
