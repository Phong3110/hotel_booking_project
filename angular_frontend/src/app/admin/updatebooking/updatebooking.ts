import { Component, ChangeDetectorRef } from '@angular/core';
import { Api } from '../../service/api';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-updatebooking',
  imports: [CommonModule, FormsModule],
  templateUrl: './updatebooking.html',
  styleUrl: './updatebooking.css',
})
export class Updatebooking {
  bookingCode: string = ''; // Booking reference from the URL
  bookingDetails: any = null; // Store booking details

  formState = {
    id: '',
    bookingStatus: '',
    paymentStatus: ''
  }; // Form state to update booking status

  message = ""
  error = ""

  constructor(
    private api: Api,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Get the booking code from the URL parameter
    this.bookingCode = this.activatedRoute.snapshot.paramMap.get('bookingCode') || '';
    
    // Fetch booking details using the API service
    this.fetchBookingDetails();
  }

  showError(msg: string) {
    this.error = msg;
    setTimeout(() => {
      this.error = "";
    }, 4000);
  }

  // Fetch the booking details from the API
  fetchBookingDetails(): void {
    this.api.getBookingByReference(this.bookingCode).subscribe({
      next:(response: any) => {
        this.bookingDetails = response.booking;
        this.formState = {
          id: this.bookingDetails.id,
          bookingStatus: this.bookingDetails.bookingStatus || '',
          paymentStatus: this.bookingDetails.paymentStatus || ''
        };
        this.cdr.detectChanges();
      },
      error:(error) => {
        this.showError(error.error?.message || error.message );
        this.cdr.detectChanges();
      }
    });
  }

  // Handle form input changes (for booking status and payment status)
  handleChange(event: any): void {
    const { name, value } = event.target;
    this.formState = { ...this.formState, [name]: value };
  }

  // Handle form submission to update the booking status
  handleUpdate(): void {
    if (!this.formState.bookingStatus && !this.formState.paymentStatus) {
      this.showError('Please update at least one field.');
      return;
    }

    this.api.updateBooking(this.formState).subscribe({
      next: () => {
        this.message = 'Booking updated successfully.';
        setTimeout(() => {
          this.message = "";
          this.router.navigate(['/admin/manage-bookings']);
        }, 3000);
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.showError(error.error?.message || error.message);
        this.cdr.detectChanges();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  // Check if the booking details are still loading
  get isLoading(): boolean {
    return !this.bookingDetails;
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
}