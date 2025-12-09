import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Roomresult } from '../../roomresult/roomresult';
import { Pagination } from '../../pagination/pagination';
import { Api } from '../../service/api';
import { Router } from '@angular/router';

@Component({
  selector: 'app-managerooms',
  imports: [CommonModule, FormsModule, Roomresult, Pagination],
  templateUrl: './managerooms.html',
  styleUrl: './managerooms.css',
})
export class Managerooms {
  rooms: any[] = [];
  filteredRooms: any[] = [];
  roomTypes: string[] = [];
  selectedRoomType: string = '';
  currentPage: number = 1;
  roomsPerPage: number = 5;
  error:any = null;

  constructor(private api: Api, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.fetchRooms();
    this.fetchRoomTypes();
  }

  showError(msg: string) {
    this.error = msg;
    setTimeout(() => {
      this.error = "";
    }, 4000);
  }
  
  // Fetch all rooms from the API
  fetchRooms() {
    this.api.getAllRooms().subscribe({
      next: (response: any) => {
        this.rooms = response.rooms;
        this.filteredRooms = response.rooms;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.showError('Error fetching rooms:' + error);
        this.cdr.detectChanges();
      }
    });
  }

  // Fetch room types from the API
  fetchRoomTypes() {
    this.api.getRoomTypes().subscribe({
      next: (types: string[]) => {
        this.roomTypes = types;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.showError('Error fetching room types: ' + error);
        this.cdr.detectChanges();
      },
    });
  }

  // Handle changes to room type filter
  handleRoomTypeChange(event: any) {
    const selectedType = event.target.value;
    this.selectedRoomType = selectedType;
    this.filterRooms(selectedType);
  }

  // Filter rooms by type
  filterRooms(type: string) {
    if (type === '') {
      this.filteredRooms = this.rooms;
    } else {
      this.filteredRooms = this.rooms.filter((room) => room.type === type);
    }
    this.currentPage = 1; // Reset to first page when filter changes
  }

  // Pagination logic
  get indexOfLastRoom() {
    return this.currentPage * this.roomsPerPage;
  }

  get indexOfFirstRoom() {
    return this.indexOfLastRoom - this.roomsPerPage;
  }

  get currentRooms() {
    return this.filteredRooms.slice(this.indexOfFirstRoom, this.indexOfLastRoom);
  }

  // Pagination function to change page
  paginate(pageNumber: number) {
    this.currentPage = pageNumber;
  }

  // Navigate to Add Room page
  navigateToAddRoom() {
    this.router.navigate(['/admin/add-room']);
  }
}