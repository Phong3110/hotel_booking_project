package com.example.HotelBooking.services.impl;

import com.example.HotelBooking.dtos.*;
import com.example.HotelBooking.entities.*;
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
        List<Booking> bookingList = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
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

            // Validate check-out date không quá xa (max 1 năm)
            if (bookingDTO.getCheckOutDate().isAfter(LocalDate.now().plusYears(1))) {
                throw new InvalidBookingStateAndDateException("Cannot book more than 1 year in advance");
            }

            // Validate booking duration (min 1 night, max 30 nights)
            long nights = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
            if (nights > 30) {
                throw new InvalidBookingStateAndDateException("Maximum stay is 30 nights");
            }

            // Prevent spam - check user's pending bookings
            long pendingCount = bookingRepository.countByUserAndPaymentStatus(
                    currentUser, PaymentStatus.PENDING
            );
            if (pendingCount >= 3) {
                throw new InvalidBookingStateAndDateException("You have too many pending bookings. Please complete or cancel them first.");
            }

            Room room = roomRepository.findById(bookingDTO.getRoom().getId())
                    .orElseThrow(()-> new NotFoundException("Room Not Found"));

            // Validation: Ensure the check-in date is not before today
            if (bookingDTO.getCheckInDate().isBefore(LocalDate.now())){
                throw new InvalidBookingStateAndDateException("Check in date cannot be before today");
            }

            // Validation: Ensure the check-out date is not before check in date
            if (bookingDTO.getCheckOutDate().isBefore(bookingDTO.getCheckInDate())){
                throw new InvalidBookingStateAndDateException("Check out date cannot be before check in date");
            }

            // Validation: Ensure the check-in date is not same as check out date
            if (bookingDTO.getCheckInDate().isEqual(bookingDTO.getCheckOutDate())){
                throw new InvalidBookingStateAndDateException("Check in date cannot be equal to check out date");
            }

            // Validate room availability
            boolean isAvailable = roomAvailabilityService.isAvailable(room.getId(), bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
            if (!isAvailable) {
                throw new InvalidBookingStateAndDateException("Room is not available for the selected date ranges");
            }

            // QUAN TRỌNG: Lưu giá phòng tại thời điểm booking
            BigDecimal pricePerNightAtBooking = room.getPricePerNight();

            // Calculate the total price với giá hiện tại
            BigDecimal totalPrice = calculateTotalPrice(pricePerNightAtBooking, bookingDTO);

            // Validate total price
            if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBookingStateAndDateException("Invalid booking price");
            }

            String bookingReference;
            do {
                bookingReference = bookingCodeGenerator.generateBookingReference();
            } while (bookingRepository.existsByBookingReference(bookingReference));

            // Create and save the booking
            Booking booking = new Booking();

            booking.setBookingReference(bookingReference);
            booking.setCheckInDate(bookingDTO.getCheckInDate());
            booking.setCheckOutDate(bookingDTO.getCheckOutDate());
            booking.setPricePerNightAtBooking(pricePerNightAtBooking); // LƯU GIÁ
            booking.setTotalPrice(totalPrice);
            booking.setBookingStatus(BookingStatus.BOOKED);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setCreatedAt(LocalDateTime.now());
            booking.setRoom(room);
            booking.setUser(currentUser);

            bookingRepository.save(booking);

            // Save guests
            if (bookingDTO.getGuests() != null && !bookingDTO.getGuests().isEmpty()) {
                if (bookingDTO.getGuests().size() > room.getCapacity()) {
                    throw new InvalidBookingStateAndDateException(
                            "Number of guests (" + bookingDTO.getGuests().size() +
                                    ") exceeds room capacity (" + room.getCapacity() + ")"
                    );
                }

                for (GuestDTO guestDTO : bookingDTO.getGuests()) {
                    Guest guest = Guest.builder()
                            .firstName(guestDTO.getFirstName())
                            .lastName(guestDTO.getLastName())
                            .email(guestDTO.getEmail())
                            .phoneNumber(guestDTO.getPhoneNumber())
                            .identityNumber(guestDTO.getIdentityNumber())
                            .booking(booking)
                            .build();
                    booking.addGuest(guest);
                }

                bookingRepository.save(booking);
            }

            // Reserve dates
            try {
                roomAvailabilityService.bookRoomDates(room, booking.getCheckInDate(), booking.getCheckOutDate(), booking);
            } catch (InvalidBookingStateAndDateException e) {
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

            // Send notification
            try {
                String guestInfo = "";
                if (booking.getGuests() != null && !booking.getGuests().isEmpty()) {
                    guestInfo = "\nGuests Information:\n";
                    for (Guest guest : booking.getGuests()) {
                        guestInfo += "- " + guest.getFirstName() + " " + guest.getLastName() +
                                " (" + guest.getEmail() + ")\n";
                    }
                }

                NotificationDTO notificationDTO = NotificationDTO.builder()
                        .recipient(currentUser.getEmail())
                        .subject("Booking Confirmation")
                        .body(
                                "Your booking with reference **" + bookingReference + "** has been successfully created.\n" +
                                        "Price per night: $" + pricePerNightAtBooking + "\n" +
                                        "Total price: $" + totalPrice + "\n" +
                                        "Please proceed with your payment using the payment link below:\n" +
                                        paymentUrl + guestInfo
                        )
                        .bookingReference(bookingReference)
                        .build();

                notificationService.sendEmail(notificationDTO);
            } catch (Exception e) {
                log.error("Failed to send booking confirmation email for booking: {}", bookingReference, e);
            }

            BookingDTO responseDTO = modelMapper.map(booking, BookingDTO.class);
            return Response.builder()
                    .status(200)
                    .message("Booking created successfully")
                    .booking(responseDTO)
                    .build();

        } catch (InvalidBookingStateAndDateException | NotFoundException e) {
            log.warn("Booking creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
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
        return Response.builder()
                .status(200)
                .message("success")
                .booking(bookingDTO)
                .build();
    }

    @Override
    @Transactional
    public Response updateBooking(BookingDTO bookingDTO) {
        if (bookingDTO.getId() == null) throw new NotFoundException("Booking id is required");

        Booking existingBooking = bookingRepository.findById(bookingDTO.getId())
                .orElseThrow(()-> new NotFoundException("Booking Not Found"));

        // Validation rules cho update booking status
        if (bookingDTO.getBookingStatus() != null) {
            BookingStatus currentStatus = existingBooking.getBookingStatus();
            BookingStatus newStatus = bookingDTO.getBookingStatus();

            // 1. Không thể update nếu đã CANCELLED
            if (currentStatus == BookingStatus.CANCELLED) {
                throw new InvalidBookingStateAndDateException("Cannot update a cancelled booking");
            }

            // 2. Không thể CANCEL nếu đã CHECKED_IN
            if (newStatus == BookingStatus.CANCELLED && currentStatus == BookingStatus.CHECKED_IN) {
                throw new InvalidBookingStateAndDateException("Cannot cancel a booking that is checked in");
            }

            // 3. Không thể CHECKED_IN nếu chưa đến ngày check-in
            if (newStatus == BookingStatus.CHECKED_IN) {
                if (LocalDate.now().isBefore(existingBooking.getCheckInDate())) {
                    throw new InvalidBookingStateAndDateException("Cannot check in before check-in date");
                }
                // Phải đã PAID
                if (existingBooking.getPaymentStatus() != PaymentStatus.PAID) {
                    throw new InvalidBookingStateAndDateException("Cannot check in without payment");
                }
            }

            // 4. Không thể CHECKED_OUT nếu chưa CHECKED_IN
            if (newStatus == BookingStatus.CHECKED_OUT && currentStatus != BookingStatus.CHECKED_IN) {
                throw new InvalidBookingStateAndDateException("Must be checked in before checking out");
            }

            // 5. Không thể CANCEL sau check-in date
            if (newStatus == BookingStatus.CANCELLED &&
                    !LocalDate.now().isBefore(existingBooking.getCheckInDate())) {
                throw new InvalidBookingStateAndDateException("Cannot cancel after check-in date has passed");
            }

            // Nếu cancel, release room dates
            if (newStatus == BookingStatus.CANCELLED) {
                roomAvailabilityService.releaseRoomDates(
                        existingBooking.getRoom(),
                        existingBooking.getCheckInDate(),
                        existingBooking.getCheckOutDate()
                );
            }

            existingBooking.setBookingStatus(newStatus);
        }

        // Validation rules cho payment status
        if (bookingDTO.getPaymentStatus() != null) {
            PaymentStatus currentPayment = existingBooking.getPaymentStatus();
            PaymentStatus newPayment = bookingDTO.getPaymentStatus();

            // 1. Không thể thay đổi nếu đã PAID (chỉ có thể REFUNDED)
            if (currentPayment == PaymentStatus.PAID && newPayment != PaymentStatus.REFUNDED) {
                throw new InvalidBookingStateAndDateException("Cannot change payment status from PAID except to REFUNDED");
            }

            // 2. Không thể REFUNDED nếu chưa PAID
            if (newPayment == PaymentStatus.REFUNDED && currentPayment != PaymentStatus.PAID) {
                throw new InvalidBookingStateAndDateException("Cannot refund a payment that hasn't been made");
            }

            // 3. Nếu set CANCELLED hoặc REFUNDED, booking status cũng phải CANCELLED
            if ((newPayment == PaymentStatus.CANCELLED || newPayment == PaymentStatus.REFUNDED) &&
                    existingBooking.getBookingStatus() != BookingStatus.CANCELLED) {
                existingBooking.setBookingStatus(BookingStatus.CANCELLED);

                // Release room dates
                roomAvailabilityService.releaseRoomDates(
                        existingBooking.getRoom(),
                        existingBooking.getCheckInDate(),
                        existingBooking.getCheckOutDate()
                );
            }

            existingBooking.setPaymentStatus(newPayment);
        }

        bookingRepository.save(existingBooking);

        return Response.builder()
                .status(200)
                .message("Booking Updated Successfully")
                .build();
    }

    // Helper method - tính tổng giá dựa trên giá per night được truyền vào
    private BigDecimal calculateTotalPrice(BigDecimal pricePerNight, BookingDTO bookingDTO){
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

        // 4. Cannot cancel if currently staying
        LocalDate today = LocalDate.now();
        if (!today.isBefore(booking.getCheckInDate()) && today.isBefore(booking.getCheckOutDate())) {
            throw new InvalidBookingStateAndDateException("Cannot cancel booking during your stay");
        }

        // 5. Cancellation policy - must cancel 24h before check-in
        if (booking.getCheckInDate().minusDays(1).isBefore(LocalDate.now())) {
            throw new InvalidBookingStateAndDateException("Cancellation must be made at least 24 hours before check-in date");
        }

        // Release room dates
        roomAvailabilityService.releaseRoomDates(booking.getRoom(), booking.getCheckInDate(), booking.getCheckOutDate());

        // Update statuses
        booking.setBookingStatus(BookingStatus.CANCELLED);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
        }

        bookingRepository.save(booking);

        // Send notification
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
        }

        BookingDTO bookingDTO = modelMapper.map(booking, BookingDTO.class);

        return Response.builder()
                .status(200)
                .message("Booking cancelled successfully")
                .booking(bookingDTO)
                .build();
    }
}