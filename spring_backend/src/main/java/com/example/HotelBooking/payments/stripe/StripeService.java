package com.example.HotelBooking.payments.stripe;


import com.example.HotelBooking.dtos.NotificationDTO;
import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.PaymentEntity;
import com.example.HotelBooking.enums.NotificationType;
import com.example.HotelBooking.enums.PaymentGateway;
import com.example.HotelBooking.enums.PaymentStatus;
import com.example.HotelBooking.exceptions.NotFoundException;
import com.example.HotelBooking.payments.dto.PaymentRequest;
import com.example.HotelBooking.repositories.BookingRepository;
import com.example.HotelBooking.repositories.PaymentRepository;
import com.example.HotelBooking.services.NotificationService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Value("${stripe.api.secret.key}")
    private String secreteKey;

    public String createPaymentIntent(PaymentRequest paymentRequest){
        Stripe.apiKey = secreteKey;
        String bookingReference = paymentRequest.getBookingReference();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking Not Found"));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new NotFoundException("Payment already made for this booking");
        }

        try{
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(paymentRequest.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) //amount cents
                    .setCurrency("usd")
                    .putMetadata("bookingReference", bookingReference)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return intent.getClientSecret();

        }catch (Exception e){
            throw new RuntimeException("Error creating payment intent: " + e.getMessage());
        }
    }

    @Transactional
    public void updatePaymentBooking(PaymentRequest paymentRequest) {
        String bookingReference = paymentRequest.getBookingReference();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking Not Found"));

        // THÊM validation này:
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Payment already completed for this booking");
        }

        // THÊM: Check duplicate transactionId (Idempotency)
        if (paymentRequest.isSuccess() && paymentRequest.getTransactionId() != null) {
            boolean exists = paymentRepository.existsByTransactionId(paymentRequest.getTransactionId());
            if (exists) {
                throw new RuntimeException("This transaction has already been processed");
            }
        }

        // Tạo payment record
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentGateway(PaymentGateway.STRIPE);
        payment.setAmount(paymentRequest.getAmount());
        payment.setTransactionId(paymentRequest.getTransactionId());
        payment.setPaymentStatus(paymentRequest.isSuccess() ? PaymentStatus.PAID : PaymentStatus.FAILED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setBookingReference(bookingReference);
        payment.setUser(booking.getUser());

        if (!paymentRequest.isSuccess()) {
            payment.setFailureReason(paymentRequest.getFailureReason());
        }

        paymentRepository.save(payment); //save payment to database

        // Notification DTO
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(booking.getUser().getEmail())
                .type(NotificationType.EMAIL)
                .bookingReference(bookingReference)
                .build();

        if (paymentRequest.isSuccess()) {
            // Update booking status
            booking.setPaymentStatus(PaymentStatus.PAID);
            bookingRepository.save(booking); //Update the booking

            // Send success notification
            notificationDTO.setSubject("Booking Payment Successful");
            notificationDTO.setBody("Congratulations! Your payment for booking " + bookingReference + " is successful.");
            notificationService.sendEmail(notificationDTO); //send email
        } else {
            // Update booking status
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking); //Update the booking

            // Send failure notification
            notificationDTO.setSubject("Booking Payment Failed");
            notificationDTO.setBody("Your payment for booking " + bookingReference + " failed. Reason: " + paymentRequest.getFailureReason());
            notificationService.sendEmail(notificationDTO); //send email
        }
    }
}
