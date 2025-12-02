package com.example.HotelBooking.payments.paypal;

import com.example.HotelBooking.dtos.NotificationDTO;
import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.PaymentEntity;
import com.example.HotelBooking.enums.NotificationType;
import com.example.HotelBooking.enums.PaymentGateway;
import com.example.HotelBooking.enums.PaymentStatus;
import com.example.HotelBooking.exceptions.NotFoundException;
import com.example.HotelBooking.payments.dto.PaymentRequest;
import com.example.HotelBooking.payments.dto.PaymentResponse;
import com.example.HotelBooking.repositories.BookingRepository;
import com.example.HotelBooking.repositories.PaymentRepository;
import com.example.HotelBooking.services.NotificationService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaypalService {

    private final PayPalHttpClient payPalHttpClient;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    // 1. Tạo Order trên PayPal (Tương tự createPaymentIntent của Stripe)
    public String createOrder(PaymentRequest paymentRequest) {
        String bookingReference = paymentRequest.getBookingReference();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking Not Found"));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Payment already made for this booking");
        }

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE"); // Intent là CAPTURE để trừ tiền ngay

        // Tạo thông tin số tiền
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        purchaseUnits.add(new PurchaseUnitRequest()
                .referenceId(bookingReference)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode("USD")
                        .value(paymentRequest.getAmount().toString()))); // PayPal nhận String amount

        orderRequest.purchaseUnits(purchaseUnits);

        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            // Trả về Order ID để frontend dùng
            return order.id();
        } catch (IOException e) {
            throw new RuntimeException("Error creating PayPal order");
        }
    }

    // 2. Capture Order (Xác nhận thanh toán và Lưu DB)
    // Hàm này được gọi sau khi User đăng nhập PayPal và nhấn "Pay Now"
    @Transactional
    public PaymentResponse captureOrder(String orderId) {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.requestBody(new OrderRequest());

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            // Lấy bookingReference từ purchase unit
            String bookingReference = order.purchaseUnits().get(0).referenceId();
            Booking booking = bookingRepository.findByBookingReference(bookingReference)
                    .orElseThrow(() -> new NotFoundException("Booking Not Found"));

            // THÊM: Validate booking chưa PAID
            if (booking.getPaymentStatus() == PaymentStatus.PAID) {
                log.warn("Attempted duplicate payment for booking: {}", bookingReference);
                return PaymentResponse.builder()
                        .status("ALREADY_PAID")
                        .message("Payment already completed for this booking.")
                        .build();
            }

            // Kiểm tra trạng thái từ PayPal
            if ("COMPLETED".equals(order.status())) { // PayPal capture status là "COMPLETED", không phải "PAID"
                PurchaseUnit purchaseUnit = order.purchaseUnits().get(0);

                // Kiểm tra capture details
                if (purchaseUnit.payments() == null || purchaseUnit.payments().captures() == null || purchaseUnit.payments().captures().isEmpty()) {
                    throw new RuntimeException("Missing capture details in PayPal response.");
                }

                Capture capture = purchaseUnit.payments().captures().get(0);

                // Kiểm tra amount
                if (capture.amount() == null || capture.amount().value() == null) {
                    throw new RuntimeException("Payment amount could not be determined after capture.");
                }

                // THÊM: Check duplicate transactionId
                String captureId = capture.id(); // PayPal capture ID
                if (paymentRepository.existsByTransactionId(captureId)) {
                    return PaymentResponse.builder()
                            .status("DUPLICATE")
                            .message("This transaction has already been processed.")
                            .build();
                }

                // Lưu PaymentEntity
                PaymentEntity payment = new PaymentEntity();
                payment.setPaymentGateway(PaymentGateway.PAYPAL);
                payment.setAmount(new BigDecimal(capture.amount().value()));
                payment.setTransactionId(captureId); // Dùng capture.id() thay vì order.id()
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setBookingReference(bookingReference);
                payment.setUser(booking.getUser());

                paymentRepository.save(payment);

                // Cập nhật Booking
                booking.setPaymentStatus(PaymentStatus.PAID);
                bookingRepository.save(booking);

                // Gửi Email thành công
                sendNotification(booking, true, null);

                return PaymentResponse.builder()
                        .status("SUCCESS")
                        .message("Payment captured and booking updated successfully.")
                        .build();
            } else {
                // Lưu failed payment record
                PaymentEntity failedPayment = new PaymentEntity();
                failedPayment.setPaymentGateway(PaymentGateway.PAYPAL);
                failedPayment.setTransactionId(orderId);
                failedPayment.setPaymentStatus(PaymentStatus.FAILED);
                failedPayment.setPaymentDate(LocalDateTime.now());
                failedPayment.setBookingReference(bookingReference);
                failedPayment.setUser(booking.getUser());
                failedPayment.setFailureReason("PayPal order status: " + order.status());

                paymentRepository.save(failedPayment);

                // Update booking
                booking.setPaymentStatus(PaymentStatus.FAILED);
                bookingRepository.save(booking);

                // Send failure notification
                sendNotification(booking, false, "Payment status: " + order.status());

                return PaymentResponse.builder()
                        .status("FAILURE")
                        .message("PayPal payment not completed. Status: " + order.status())
                        .build();
            }

        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            // Xử lý trường hợp lỗi (có thể lưu log failure)
            throw new RuntimeException("Payment processing failed internally: " + e.getMessage());
        }
    }

    private void sendNotification(Booking booking, boolean isSuccess, String failureReason) {
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(booking.getUser().getEmail())
                .type(NotificationType.EMAIL)
                .bookingReference(booking.getBookingReference())
                .build();

        if (isSuccess) {
            notificationDTO.setSubject("Booking Payment Successful (PayPal)");
            notificationDTO.setBody("Congratulations!! Your payment for booking: " + booking.getBookingReference() + " is successful via PayPal.");
        } else {
            notificationDTO.setSubject("Booking Payment Failed (PayPal)");
            notificationDTO.setBody("Your payment failed. Reason: " + failureReason);
        }
        notificationService.sendEmail(notificationDTO);
    }
}