import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../../service/api';
import { Router } from '@angular/router';

@Component({
  selector: 'app-adminhome',
  imports: [CommonModule],
  templateUrl: './adminhome.html',
  styleUrl: './adminhome.css',
})
export class Adminhome {
  adminName: string = '';
  error: string | null = null;

  constructor(private api: Api, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.fetchAdminName();
  }

  // Fetch the admin's profile name
  fetchAdminName(): void {
    this.api.myProfile().subscribe({
      next: (resp: any) => {
        this.adminName = resp.user.firstName;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.error = error.message;
        console.error('Error fetching admin name:', error);
        this.cdr.detectChanges();
      },
    });
  }

  // Navigate to Manage Rooms
  navigateToManageRooms(): void {
    this.router.navigate(['/admin/manage-rooms']);
  }

  // Navigate to Manage Bookings
  navigateToManageBookings(): void {
    this.router.navigate(['/admin/manage-bookings']);
  }

  // Navigate to Admin Register
  navigateToAdminRegister(): void {
    this.router.navigate(['/admin/admin-register']);
  }
}