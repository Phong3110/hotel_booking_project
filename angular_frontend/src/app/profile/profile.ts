import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../service/api';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  user: any = null;
  bookings: any[] = [];
  error: any = null;

  constructor(private api: Api, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.fetchUserProfile();
  }

  // Fetch user profile and bookings
  fetchUserProfile() {
    this.api.myProfile().subscribe({
      next: (response: any) => {
        this.user = response.user;
        // Fetch bookings after the user profile is fetched
        this.api.myBookings().subscribe({
          next: (bookingResponse: any) => {
            this.bookings = bookingResponse.bookings;
            this.cdr.detectChanges(); // force update view
          },
          error: (err) => {
            this.showError(
              err?.error?.message ||
                err?.error ||
                'Error getting my bookings: ' + err
            );
            this.cdr.detectChanges(); // force update view
          },
        });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.showError(
          err?.error?.message ||
            err?.error ||
            'Error getting my profile info: ' + err
        );
        this.cdr.detectChanges();
      },
    });
  }

  // Handle errors
  showError(msg: string) {
    this.error = msg;
    setTimeout(() => {
      this.error = null;
    }, 4000);
  }

  // Handle logout
  handleLogout() {
    this.api.logout();
    this.router.navigate(['/home']);
  }

  // Navigate to edit profile page
  handleEditProfile() {
    this.router.navigate(['/edit-profile']);
  }

}
