import { Component, ChangeDetectorRef } from '@angular/core';
import { Api } from '../service/api';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-findbooking',
  imports: [CommonModule, FormsModule],
  templateUrl: './findbooking.html',
  styleUrl: './findbooking.css',
})
export class Findbooking {
  constructor(private api: Api, private route: ActivatedRoute, private router: Router, private cdr: ChangeDetectorRef){}
  
  confirmationCode: string = '';
  bookingDetails: any = null;
  message: any = null;
  error: any = null;

  handleSearch(){
    if (!this.confirmationCode.trim()) {
      this.showError("Please enter the booking confirmation Code");
      return;
    }

    this.api.getBookingByReference(this.confirmationCode).subscribe({
      next: (res) => {
        this.bookingDetails = res.booking;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.bookingDetails = null;
        this.showError(err?.error.message || "Error fetching booking details")
        this.cdr.detectChanges();
      },
    })
  }

  // Handle cancel booking
  handleCancelBooking(): void {
    if (!this.canCancelBooking()) {
      this.showError(this.getCancellationReason());
      return;
    }

    if (
      !window.confirm(
        'Are you sure you want to cancel this booking?\n\n' +
        'Cancellation Policy:\n' +
        '- Must cancel at least 24 hours before check-in\n' +
        '- Full refund for paid bookings'
      )
    ) {
      return;
    }

    this.api.cancelBooking(this.confirmationCode).subscribe({
      next: (res) => {
        if (res.status === 200) {
          this.message = "Your booking has been successfully cancelled.\nAn Email of Booking Cancellation Confirmation has been sent to you.";
          setTimeout(()=>{
            this.message = null;
            this.router.navigate(['/profile'])
          }, 8000)
        }
        this.cdr.detectChanges();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.showError(err?.error?.message || 'Error Cancel Booking');
        this.cdr.detectChanges();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
    });
  }

  // Check if booking can be cancelled
  canCancelBooking(): boolean {
    if (!this.bookingDetails) return false;

    const status = this.bookingDetails.bookingStatus;
    const paymentStatus = this.bookingDetails.paymentStatus;
    const checkInDate = new Date(this.bookingDetails.checkInDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    checkInDate.setHours(0, 0, 0, 0);

    // Cannot cancel if already cancelled
    if (status === 'CANCELLED') return false;

    // Cannot cancel if checked in
    if (status === 'CHECKED_IN') return false;

    // Cannot cancel if check-in date has passed
    if (checkInDate < today) return false;

    // Must cancel at least 24h before check-in
    const oneDayBeforeCheckIn = new Date(checkInDate);
    oneDayBeforeCheckIn.setDate(oneDayBeforeCheckIn.getDate() - 1);
    if (today > oneDayBeforeCheckIn) return false;

    return true;
  }

  // Get reason why booking cannot be cancelled
  getCancellationReason(): string {
    if (!this.bookingDetails) return 'No booking details available';

    const status = this.bookingDetails.bookingStatus;
    const checkInDate = new Date(this.bookingDetails.checkInDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    checkInDate.setHours(0, 0, 0, 0);

    if (status === 'CANCELLED') {
      return 'Booking is already cancelled';
    }

    if (status === 'CHECKED_IN') {
      return 'Cannot cancel after check-in';
    }

    if (status === 'CHECKED_OUT') {
      return 'Booking is already completed';
    }

    if (checkInDate < today) {
      return 'Check-in date has passed';
    }

    const oneDayBeforeCheckIn = new Date(checkInDate);
    oneDayBeforeCheckIn.setDate(oneDayBeforeCheckIn.getDate() - 1);
    if (today > oneDayBeforeCheckIn) {
      return 'Must cancel at least 24 hours before check-in';
    }

    return 'Unknown reason';
  }

  // Helper method để style payment status
  getPaymentStatusClass(status: string): string {
    switch (status) {
      case 'PAID':
        return 'bg-green-100 text-green-700';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-700';
      case 'CANCELLED':
      case 'FAILED':
        return 'bg-red-100 text-red-700';
      case 'REFUNDED':
        return 'bg-blue-100 text-blue-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }

  // Helper method để style booking status
  getBookingStatusClass(status: string): string {
    switch (status) {
      case 'BOOKED':
        return 'bg-blue-100 text-blue-700';
      case 'CHECKED_IN':
        return 'bg-green-100 text-green-700';
      case 'CHECKED_OUT':
        return 'bg-gray-100 text-gray-700';
      case 'CANCELLED':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }

  showError(err: any): void{
    console.log(err)
    this.error = err;
    setTimeout(() => {
      this.error = ''
    }, 4000)
  }
}