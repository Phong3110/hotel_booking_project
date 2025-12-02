package com.example.HotelBooking.services;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class BookingCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
    private static final int LENGTH = 10;

    public String generateBookingReference() {
        StringBuilder sb = new StringBuilder(LENGTH);
        Random random = new Random();
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
