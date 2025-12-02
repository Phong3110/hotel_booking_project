import { Component, ChangeDetectorRef } from '@angular/core';
import { Pagination } from '../pagination/pagination';
import { Roomresult } from '../roomresult/roomresult';
import { Roomsearch } from '../roomsearch/roomsearch';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../service/api';

@Component({
  selector: 'app-rooms',
  imports: [Pagination, Roomresult, Roomsearch, CommonModule, FormsModule],
  templateUrl: './rooms.html',
  styleUrl: './rooms.css',
})
export class Rooms {
  rooms: any[] = [];
  filteredRooms: any[] = [];
  roomTypes: string[] = [];
  selectedRoomType: string = '';
  currentPage: number = 1;
  roomsPerPage: number = 8;
  error: any = null;

  constructor(private api: Api, private cdr: ChangeDetectorRef) {}
  
  ngOnInit():void{

    this.fetchRooms();
    this.fetchRoomTypes();

  }

  showError(msg: string): void {
    this.error = msg;
    setTimeout(() => {
      this.error = null;
    }, 5000);
  }

  // Fetch all rooms from the API
  fetchRooms() {
    this.api.getAllRooms().subscribe({
      next: (response: any) => {
        this.rooms = response.rooms;
        this.filteredRooms = response.rooms;
        this.cdr.detectChanges(); // force Angular update UI
      },
      error: (err) => {
        this.showError(err?.error?.message || 'Error fetching rooms:' + err);
        this.cdr.detectChanges();
      },
    });
  }

  // Fetch room types from the API
  fetchRoomTypes() {
    this.api.getRoomTypes().subscribe({
      next: (types: string[]) => {
        this.roomTypes = types;
        this.cdr.detectChanges(); // force update UI
      },
      error: (err) => {
        this.showError(
          err?.error?.message || 'Error fetching room Types:' + err
        );
        this.cdr.detectChanges();
      },
    });
  }

  // Handle the search result passed from RoomsearchComponent
  handleSearchResult(results: any[]) {
    this.rooms = results;
    this.filteredRooms = results;
    this.cdr.detectChanges();
  }

  // Pagination logic
  get indexOfLastRoom() {
    return this.currentPage * this.roomsPerPage;
  }

  get indexOfFirstRoom() {
    return this.indexOfLastRoom - this.roomsPerPage;
  }

  get currentRooms() {
    return this.filteredRooms.slice(
      this.indexOfFirstRoom,
      this.indexOfLastRoom
    );
  }

  // Pagination function to change page
  paginate(pageNumber: number) {
    this.currentPage = pageNumber;
  }
}