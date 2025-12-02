package com.example.HotelBooking.payments.stripe;

import com.example.HotelBooking.payments.PaymentValidationService;
import com.example.HotelBooking.payments.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;
    private final PaymentValidationService paymentValidationService;

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestParam String token, @RequestBody PaymentRequest paymentRequest){
        // Validate token trước khi tạo payment intent
        paymentValidationService.validatePaymentToken(token);

        String clientSecret = stripeService.createPaymentIntent(paymentRequest);

        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updatePaymentBooking(@RequestParam String token, @RequestBody PaymentRequest paymentRequest){
        // Validate lần nữa để đảm bảo
        paymentValidationService.validatePaymentToken(token);

        // Update booking
        stripeService.updatePaymentBooking(paymentRequest);

        return ResponseEntity.ok().build();
    }
}
