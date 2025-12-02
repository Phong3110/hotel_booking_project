import { Component, EventEmitter, OnInit, Output, ChangeDetectorRef } from '@angular/core';
import { Api } from '../service/api';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'

@Component({
  selector: 'app-roomsearch',
  imports: [CommonModule, FormsModule],
  templateUrl: './roomsearch.html',
  styleUrl: './roomsearch.css',
})
export class Roomsearch implements OnInit {
  @Output() searchResults = new EventEmitter<any[]>(); // Emit the results

  startDate: string | null = null; // Store date as string
  endDate: string | null = null; // Store date as string
  roomType: string = ''; // Selected room type
  roomTypes: string[] = []; // Available room types
  error: any = null;

  minDate: string = new Date().toISOString().split('T')[0]; // Current date in 'yyyy-MM-dd' format

  constructor(private api: Api, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.fetchRoomTypes();
  }

  fetchRoomTypes() {
    this.api.getRoomTypes().subscribe({
      next: (types: any) => {
        this.roomTypes = types;
        this.cdr.detectChanges(); // ensure dropdown updates
      },
      error: (err:any) => {
        this.showError(
          err?.error?.message || 'Error Fetching Room Types: ' + err
        );
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }

  showError(msg: string): void {
    this.error = msg;
    setTimeout(() => {
      this.error = null;
    }, 5000);
  }

  handleSearch() {
    if (!this.startDate || !this.endDate || !this.roomType) {
      this.showError('Please select all fields');
      return;
    }

    // Convert startDate and endDate from string to Date
    const formattedStartDate = new Date(this.startDate);
    const formattedEndDate = new Date(this.endDate);

    // Check if the dates are valid
    if (
      isNaN(formattedStartDate.getTime()) ||
      isNaN(formattedEndDate.getTime())
    ) {
      this.showError('Invalid date format');
      return;
    }

    // Convert the Date objects to 'yyyy-MM-dd' format
    const startDateStr = formattedStartDate.toLocaleDateString('en-CA'); // 'yyyy-MM-dd'
    const endDateStr = formattedEndDate.toLocaleDateString('en-CA'); // 'yyyy-MM-dd'

    console.log('formattedStartDate: ' + startDateStr);
    console.log('formattedEndDate: ' + endDateStr);
    console.log('roomType: ' + this.roomType);

    this.api.getAvailableRooms(startDateStr, endDateStr, this.roomType).subscribe({
      next: (resp: any) => {
        if (resp.rooms.length === 0) {
          this.searchResults.emit([]); // reset parent
          this.showError(
            'Room type not currently available for the selected date'
          );
          this.cdr.detectChanges(); // force update view
          return;
        }
        this.searchResults.emit(resp.rooms); // Emit the room data
        this.error = ''; // Clear any previous errors
        this.cdr.detectChanges(); // force update view
      },
      error: (error:any) => {
        this.showError(error?.error?.message || error.message);
        this.cdr.detectChanges();
      },
    });
  }
}