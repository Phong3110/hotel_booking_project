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
    if (
      !window.confirm(
        'Are you sure you want to cancel this booking?'
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

  showError(err: any): void{
    console.log(err)
    this.error = err;
    setTimeout(() => {
      this.error = ''
    }, 4000)
  }
}