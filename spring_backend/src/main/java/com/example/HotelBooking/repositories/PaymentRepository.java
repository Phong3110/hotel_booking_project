package com.example.HotelBooking.repositories;

import com.example.HotelBooking.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    // THÊM method này để check duplicate transactionId
    boolean existsByTransactionId(String transactionId);
}
