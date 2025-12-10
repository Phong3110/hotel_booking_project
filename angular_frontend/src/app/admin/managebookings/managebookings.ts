import { Component, ChangeDetectorRef } from '@angular/core';
import { Api } from '../../service/api';
import { Router } from '@angular/router';
import { Pagination } from '../../pagination/pagination';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-managebookings',
  imports: [Pagination, CommonModule, FormsModule],
  templateUrl: './managebookings.html',
  styleUrl: './managebookings.css',
})
export class Managebookings {
  bookings: any[] = []; // Store all bookings
  filteredBookings: any[] = []; // Store filtered bookings based on search term
  searchTerm: string = ''; // Search term for filtering bookings
  currentPage: number = 1; // Current page for pagination
  bookingsPerPage: number = 5; // Number of bookings per page
  error: any = null;

  constructor(private api: Api, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.fetchBookings();
  }

  // Fetch bookings data from the API
  fetchBookings(): void {
    this.api.getAllBookings().subscribe({
      next: (response: any) => {
        this.bookings = response.bookings || []; // Set bookings or an empty array if no data
        this.filteredBookings = this.bookings; // Initially, filtered bookings are the same as all bookings
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.showError(error?.error?.message || 'Error fetching bookings: ' + error.message);
        this.cdr.detectChanges();
      }
    });
  }

  // Update filtered bookings based on the search term
  handleSearchChange(): void {
    if (!this.searchTerm) {
      this.filteredBookings = this.bookings; // If no search term, show all bookings
    } else {
      this.filteredBookings = this.bookings.filter((booking) =>
        booking.bookingReference?.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    this.currentPage = 1; // Reset to the first page when search term changes
  }

  // Handle page changes for pagination (this is the handler for the paginate event)
  onPageChange(pageNumber: number): void {
    this.currentPage = pageNumber;
  }

  // Get bookings for the current page
  get currentBookings(): any[] {
    const indexOfLastBooking = this.currentPage * this.bookingsPerPage;
    const indexOfFirstBooking = indexOfLastBooking - this.bookingsPerPage;
    return this.filteredBookings.slice(indexOfFirstBooking, indexOfLastBooking);
  }

  // Navigate to the booking management page
  manageBooking(bookingReference: string): void {
    this.router.navigate([`/admin/edit-booking/${bookingReference}`]);
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

  showError(msg: string): void {
    this.error = msg;
    setTimeout(() => {
      this.error = null;
    }, 5000);
  }
}