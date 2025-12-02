import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../service/api';
import { Router } from '@angular/router';

@Component({
  selector: 'app-roomresult',
  imports: [CommonModule],
  templateUrl: './roomresult.html',
  styleUrl: './roomresult.css',
})
export class Roomresult {
  @Input() roomSearchResults: any[] = []; // Input property for room results
  isAdmin: boolean;

  constructor(private router: Router, private api: Api) {
    // Get the current user's admin status
    this.isAdmin = this.api.isAdmin();
  }

  // Method to navigate to the edit room page (for admins)
  navigateToEditRoom(roomId: string) {
    this.router.navigate([`/admin/edit-room/${roomId}`]);
  }

  // Method to navigate to the room details page (for users)
  navigateToRoomDetails(roomId: string) {
    this.router.navigate([`/room-details/${roomId}`]);
  }
}