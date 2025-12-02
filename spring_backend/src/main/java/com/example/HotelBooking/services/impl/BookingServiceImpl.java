package com.example.HotelBooking.services.impl;

import com.example.HotelBooking.dtos.BookingDTO;
import com.example.HotelBooking.dtos.BookingStatusResponse;
import com.example.HotelBooking.dtos.NotificationDTO;
import com.example.HotelBooking.dtos.Response;
import com.example.HotelBooking.entities.Booking;
import com.example.HotelBooking.entities.PaymentLink;
import com.example.HotelBooking.entities.Room;
import com.example.HotelBooking.entities.User;
import com.example.HotelBooking.enums.UserRole;
import com.example.HotelBooking.enums.BookingStatus;
import com.example.HotelBooking.enums.PaymentStatus;
import com.example.HotelBooking.exceptions.InvalidBookingStateAndDateException;
import com.example.HotelBooking.exceptions.NotFoundException;
import com.example.HotelBooking.repositories.BookingRepository;
import com.example.HotelBooking.repositories.PaymentLinkRepository;
import com.example.HotelBooking.repositories.RoomRepository;
import com.example.HotelBooking.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final PaymentLinkRepository paymentLinkRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final RoomAvailabilityService roomAvailabilityService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final BookingCodeGenerator bookingCodeGenerator;

    @Override
    public Response getAllBookings() {
        List<Booking> bookingList =bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<BookingDTO> bookingDTOList = modelMapper.map(bookingList, new TypeToken<List<BookingDTO>>() {}.getType());

        for(BookingDTO bookingDTO: bookingDTOList){
            bookingDTO.setUser(null);
            bookingDTO.setRoom(null);
        }

        return Response.builder()
                .status(200)
                .message("success")
                .bookings(bookingDTOList)
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public Response createBooking(BookingDTO bookingDTO) {
        try {
            User currentUser = userService.getCurrentLoggedInUser();

            // 1. Validate check-out date không quá xa (max 1 năm)
            if (bookingDTO.getCheckOutDate().isAfter(LocalDate.now().plusYears(1))) {
                throw new InvalidBookingStateAndDateException("Cannot book more than 1 year in advance");
            }

            // 2. Validate booking duration (min 1 night, max 30 nights)
            long nights = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
            if (nights > 30) {
                throw new InvalidBookingStateAndDateException("Maximum stay is 30 nights");
            }

            // 3. Prevent spam - check user's pending bookings
            long pendingCount = bookingRepository.countByUserAndPaymentStatus(
                    currentUser, PaymentStatus.PENDING
            );
            if (pendingCount >= 3) {
                throw new InvalidBookingStateAndDateException("You have too many pending bookings. Please complete or cancel them first.");
            }

            Room room = roomRepository.findById(bookingDTO.getRoom().getId())
                    .orElseThrow(()-> new NotFoundException("Room Not Found"));

            //validation: Ensure the check-in date is not before today
            if (bookingDTO.getCheckInDate().isBefore(LocalDate.now())){
                throw new InvalidBookingStateAndDateException("Check in date cannot be before today");
            }

            //validation: Ensure the check-out date is not before check in date
            if (bookingDTO.getCheckOutDate().isBefore(bookingDTO.getCheckInDate())){
                throw new InvalidBookingStateAndDateException("Check out date cannot be before check in date");
            }

            //validation: Ensure the check-in date is not same as check out date
            if (bookingDTO.getCheckInDate().isEqual(bookingDTO.getCheckOutDate())){
                throw new InvalidBookingStateAndDateException("Check in date cannot be equal to check out date");
            }

            //validate room availability
            boolean isAvailable = roomAvailabilityService.isAvailable(room.getId(), bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
            if (!isAvailable) {
                throw new InvalidBookingStateAndDateException("Room is not available for the selected date ranges");
            }

            //calculate the total price needed to pay for the stay
            BigDecimal totalPrice = calculateTotalPrice(room, bookingDTO);

            // Validate total price
            if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBookingStateAndDateException("Invalid booking price");
            }

            String bookingReference;
            do {
                bookingReference = bookingCodeGenerator.generateBookingReference();
            } while (bookingRepository.existsByBookingReference(bookingReference));

            //create and save the booking
            Booking booking = new Booking();

            booking.setBookingReference(bookingReference);
            booking.setCheckInDate(bookingDTO.getCheckInDate());
            booking.setCheckOutDate(bookingDTO.getCheckOutDate());
            booking.setTotalPrice(totalPrice);
            booking.setBookingStatus(BookingStatus.BOOKED);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setCreatedAt(LocalDateTime.now());
            booking.setRoom(room);
            booking.setUser(currentUser);

            bookingRepository.save(booking); //save to database

            // Reserve dates - nếu fail sẽ rollback toàn bộ transaction
            try {
                roomAvailabilityService.bookRoomDates(room, booking.getCheckInDate(), booking.getCheckOutDate(), booking);
            } catch (InvalidBookingStateAndDateException e) {
                // Re-throw với message rõ ràng hơn
                throw new InvalidBookingStateAndDateException("Failed to reserve room: " + e.getMessage());
            }

            // Tạo payment link
            String token;

            do {
                token = UUID.randomUUID().toString().replace("-", "")
                        + RandomStringUtils.secure().nextAlphanumeric(32);
            } while (paymentLinkRepository.existsByToken(token));

            PaymentLink link = PaymentLink.builder()
                    .token(token)
                    .booking(booking)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            paymentLinkRepository.save(link);

            String paymentUrl = "http://localhost:4200/payment?token=" + token;

            // Send notification (async recommended - không rollback nếu email fail)
            try {
                NotificationDTO notificationDTO = NotificationDTO.builder()
                        .recipient(currentUser.getEmail())
                        .subject("Booking Confirmation")
                        .body(
                                "Your booking with reference **" + bookingReference + "** has been successfully created.\n" +
                                "Please proceed with your payment using the payment link below:\n" +
                                paymentUrl
                        )
                        .bookingReference(bookingReference)
                        .build();
                notificationService.sendEmail(notificationDTO);
            } catch (Exception e) {
                log.error("Failed to send booking confirmation email for booking: {}", bookingReference, e);
                // Không throw - booking vẫn thành công dù email fail
            }

            BookingDTO responseDTO = modelMapper.map(booking, BookingDTO.class);
            return Response.builder()
                    .status(200)
                    .message("Booking created successfully")
                    .booking(responseDTO)
                    .build();

        } catch (InvalidBookingStateAndDateException | NotFoundException e) {
            // Known business exceptions
            log.warn("Booking creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Unexpected errors
            log.error("Unexpected error during booking creation", e);
            throw new RuntimeException("Failed to create booking. Please try again later.");
        }
    }

    @Override
    public Response findBookingByReference(String bookingReference) {
        User currentUser = userService.getCurrentLoggedInUser();

        Booking booking;

        if (currentUser.getRole().equals(UserRole.ADMIN)) {
            booking = bookingRepository.findByBookingReference(bookingReference)
                    .orElseThrow(()-> new NotFoundException("Booking Not Found"));
        }
        else {
            booking = bookingRepository.findByBookingReferenceAndUserEmail(bookingReference, currentUser.getEmail())
                    .orElseThrow(()-> new NotFoundException("Booking Not Found"));
        }

        BookingDTO bookingDTO = modelMapper.map(booking, BookingDTO.class);
        return  Response.builder()
                .status(200)
                .message("success")
                .booking(bookingDTO)
                .build();
    }

    @Override
    public Response updateBooking(BookingDTO bookingDTO) {
        if (bookingDTO.getId() == null) throw new NotFoundException("Booking id is required");

        Booking existingBooking = bookingRepository.findById(bookingDTO.getId())
                .orElseThrow(()-> new NotFoundException("Booking Not Found"));

        if (bookingDTO.getBookingStatus() != null) {
            existingBooking.setBookingStatus(bookingDTO.getBookingStatus());
        }

        if (bookingDTO.getPaymentStatus() != null) {
            existingBooking.setPaymentStatus(bookingDTO.getPaymentStatus());
        }

        bookingRepository.save(existingBooking);

        return Response.builder()
                .status(200)
                .message("Booking Updated Successfully")
                .build();
    }


    private BigDecimal calculateTotalPrice(Room room, BookingDTO bookingDTO){
        BigDecimal pricePerNight = room.getPricePerNight();
        long days = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
        return pricePerNight.multiply(BigDecimal.valueOf(days));
    }

    @Override
    public BookingStatusResponse checkBookingStatus(String token){
        PaymentLink link = paymentLinkRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Invalid payment token"));

        Booking booking = link.getBooking();

        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
            throw new InvalidBookingStateAndDateException("Payment link has expired. This booking has already been cancelled.");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            return BookingStatusResponse.builder()
                    .status("ALREADY_PAID")
                    .message("This booking has already been paid.")
                    .bookingReference(booking.getBookingReference())
                    .build();
        }

        if (booking.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return BookingStatusResponse.builder()
                    .status("ALREADY_REFUNDED")
                    .message("This booking has already been refunded.")
                    .bookingReference(booking.getBookingReference())
                    .build();
        }

        if (booking.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return BookingStatusResponse.builder()
                    .status("ALREADY_CANCELLED")
                    .message("This booking has already been cancelled.")
                    .bookingReference(booking.getBookingReference())
                    .build();
        }

        return BookingStatusResponse.builder()
                .status("OK")
                .message("Booking is valid and ready for payment.")
                .amount(booking.getTotalPrice())
                .bookingReference(booking.getBookingReference())
                .build();
    }

    @Override
    @Transactional
    public Response cancelBooking(String bookingReference) {
        User currentUser = userService.getCurrentLoggedInUser();

        Booking booking = bookingRepository.findByBookingReferenceAndUserEmail(bookingReference, currentUser.getEmail())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        // 1. Check already cancelled
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateAndDateException("Booking already cancelled");
        }

        // 2. Cannot cancel if already checked in
        if (booking.getBookingStatus() == BookingStatus.CHECKED_IN) {
            throw new InvalidBookingStateAndDateException("Cannot cancel a booking that has already been checked in");
        }

        // 3. Cannot cancel if check-in date has passed
        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new InvalidBookingStateAndDateException("Cannot cancel booking after check-in date has passed");
        }

        // 4. Cannot cancel if currently staying (between check-in and check-out)
        LocalDate today = LocalDate.now();
        if (!today.isBefore(booking.getCheckInDate()) && today.isBefore(booking.getCheckOutDate())) {
            throw new InvalidBookingStateAndDateException("Cannot cancel booking during your stay");
        }

        // 5. Optional: Cancellation policy - e.g., must cancel 24h before check-in
        if (booking.getCheckInDate().minusDays(1).isBefore(LocalDate.now())) {
            throw new InvalidBookingStateAndDateException("Cancellation must be made at least 24 hours before check-in date");
        }

        // release room dates
        roomAvailabilityService.releaseRoomDates(booking.getRoom(), booking.getCheckInDate(), booking.getCheckOutDate());

        // update statuses
        booking.setBookingStatus(BookingStatus.CANCELLED);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            // create refund flow or mark refunded if you do automatic refund later
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            // optionally create a PaymentEntity with refund info (transactionId null or refundId)
        } else {
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
        }

        bookingRepository.save(booking);

        // Send notification (async recommended - không rollback nếu email fail)
        try {
            NotificationDTO notificationDTO = NotificationDTO.builder()
                    .recipient(currentUser.getEmail())
                    .subject("Booking Cancellation Confirmation")
                    .body("Your booking with reference **" + bookingReference + "** has been successfully cancelled.")
                    .bookingReference(bookingReference)
                    .build();
            notificationService.sendEmail(notificationDTO);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email for booking: {}", bookingReference, e);
            // Không throw - booking vẫn thành công dù email fail
        }

        BookingDTO bookingDTO = modelMapper.map(booking, BookingDTO.class);

        return Response.builder()
                .status(200)
                .message("Booking cancelled successfully")
                .booking(bookingDTO)
                .build();
    }
}
