// spring_backend/src/main/java/com/example/HotelBooking/entities/Guest.java
package com.example.HotelBooking.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "guests")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    // Optional: Thêm thông tin bổ sung
    private String identityNumber; // CMND/CCCD

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}