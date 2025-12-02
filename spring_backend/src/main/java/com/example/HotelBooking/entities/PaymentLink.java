package com.example.HotelBooking.entities;

import com.example.HotelBooking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment_links")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Token an toàn, random
    @Column(nullable = false, unique = true)
    private String token;

    // Liên kết với booking
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private LocalDateTime expiresAt;
}
