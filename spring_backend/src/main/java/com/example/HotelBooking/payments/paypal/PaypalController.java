package com.example.HotelBooking.payments.paypal;

import com.example.HotelBooking.payments.PaymentValidationService;
import com.example.HotelBooking.payments.dto.PaymentRequest;
import com.example.HotelBooking.payments.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paypal")
@RequiredArgsConstructor
public class PaypalController {

    private final PaypalService paypalService;
    private final PaymentValidationService paymentValidationService;

    // Bước 1: Frontend gọi API này để lấy Order ID
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createOrder(@RequestParam String token, @RequestBody PaymentRequest paymentRequest) {
        // Validate token
        paymentValidationService.validatePaymentToken(token);

        String orderId = paypalService.createOrder(paymentRequest);
        return ResponseEntity.ok(Map.of("orderId", orderId));
    }

    // Bước 2: Sau khi user approve ở PayPal, Frontend gọi API này với orderID để server trừ tiền và lưu DB
    @PostMapping("/capture")
    public ResponseEntity<PaymentResponse> captureOrder(@RequestParam String token, @RequestParam String orderId) {
        // Validate token
        paymentValidationService.validatePaymentToken(token);

        PaymentResponse response = paypalService.captureOrder(orderId);

        // Invalidate token nếu thành công
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }
}