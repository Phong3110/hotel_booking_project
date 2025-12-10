import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../service/api';
import { ActivatedRoute, Router } from '@angular/router';

// Thêm interface
interface Guest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  identityNumber?: string;
}

@Component({
  selector: 'app-roomdetails',
  imports: [CommonModule, FormsModule],
  templateUrl: './roomdetails.html',
  styleUrl: './roomdetails.css',
})
export class Roomdetails {
  constructor(private api: Api, private route: ActivatedRoute, private router: Router, private cdr: ChangeDetectorRef){}

  // Thêm vào class Roomdetails
  guests: Guest[] = [];

  room: any = null;
  roomId: any = '';
  checkInDate: Date | null = null;
  checkOutDate: Date | null = null;
  totalPrice: number = 0;
  totalDaysToStay: number = 0;
  showDatePicker:boolean = false;
  showBookingPreview: boolean = false;
  message: any = null;
  error: any = null;

  //minimum date for the check-in-date
  minDate: string = new Date().toISOString().split('T')[0] //get the current date in this format "yyy-mm-dd"
  
  ngOnInit():void{
    this.roomId = this.route.snapshot.paramMap.get('id');
    
    if (this.roomId) {
      this.fetchRoomDetails(this.roomId)
    }
    
    // Lấy thông tin user hiện tại và set làm guest đầu tiên
    this.fetchCurrentUser();
  }

  fetchCurrentUser(): void {
    this.api.myProfile().subscribe({
      next: (response: any) => {
        const user = response.user;
        // Set user làm guest đầu tiên
        this.guests = [{
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          email: user.email || '',
          phoneNumber: user.phoneNumber || '',
          identityNumber: ''
        }];
        this.cdr.detectChanges();
      },
      error: (err) => {
        // Nếu không lấy được user, tạo guest trống
        this.guests = [this.createEmptyGuest()];
        this.cdr.detectChanges();
      }
    });
  }


  createEmptyGuest(): Guest {
    return {
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      identityNumber: ''
    };
  }

  addGuest(): void {
    if (this.guests.length < (this.room?.capacity || 1)) {
      this.guests.push(this.createEmptyGuest());
    }
  }

  removeGuest(index: number): void {
    if (this.guests.length > 1) {
      this.guests.splice(index, 1);
    }
  }

  validateGuests(): boolean {
    if (this.guests.length === 0) {
      this.showError('Please add at least one guest');
      return false;
    }
    
    for (let i = 0; i < this.guests.length; i++) {
      const guest = this.guests[i];
      if (!guest.firstName || !guest.lastName || !guest.email || !guest.phoneNumber || !guest.identityNumber) {
        this.showError(`Please fill all required fields for Guest ${i + 1}`);
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return false;
      }
      
      // Validate email format
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(guest.email)) {
        this.showError(`Invalid email format for Guest ${i + 1}`);
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return false;
      }
    }
    
    if (this.guests.length > (this.room?.capacity || 1)) {
      this.showError(`Number of guests cannot exceed room capacity (${this.room?.capacity})`);
      return false;
    }
    
    return true;
  }

  fetchRoomDetails(roomId: string): void{
    this.api.getRoomById(roomId).subscribe({
      next:(res: any) =>{
        this.room = res.room
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.showError(err?.error?.message || "Unable to fetch room details")
        this.cdr.detectChanges();
      }
    })
  }

  showError(err: any): void{
    console.log(err)
    this.error = err;
    setTimeout(() => {
      this.error = ''
    }, 5000)
  }

  calculateTotalPrice(): number {
    if (!this.checkInDate || !this.checkOutDate) return 0;

    //convert it date
    const checkIn = new Date(this.checkInDate)
    const checkOut = new Date(this.checkOutDate)

    if (isNaN(checkIn.getTime()) || isNaN(checkOut.getTime())) {
      this.showError("Invalid Date selected")
      return 0;
    }

    const oneDay = 24 * 60 * 60 * 1000; //milisec
    const totalDays = Math.round(Math.abs((checkOut.getTime() - checkIn.getTime()) / oneDay)); //differenc in days

    this.totalDaysToStay = totalDays;

    return this.room?.pricePerNight * totalDays || 0;
  }

  handleConfirmation(): void{
    if(!this.checkInDate || !this.checkOutDate){
      this.showError("Please select both check-in and check-out dates");
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    if (!this.validateGuests()) {
      return;
    }

    this.totalPrice = this.calculateTotalPrice();
    this.showBookingPreview = true;
  }

  acceptBooking():void{
    if(!this.room) return

    if (!this.validateGuests()) {
      return;
    }

    //Ensure the check in sarte and check out date are well formatted
    const formattedCheckInDate = this.checkInDate? new Date(this.checkInDate).toLocaleDateString('en-CA'):'';
    const formattedCheckOutDate = this.checkOutDate? new Date(this.checkOutDate).toLocaleDateString('en-CA'): '';

    //we are building our body object
    const booking = {
      checkInDate: formattedCheckInDate,
      checkOutDate: formattedCheckOutDate,
      room: this.room,
      guests: this.guests // Thêm guests vào booking
    };

    this.api.bookRoom(booking).subscribe({
      next: (res: any) =>{
        if (res.status === 200) {
          this.message = "Your Booking is Successful.\nAn Email of your booking details and the payment link has been sent to you";
          setTimeout(()=>{
            this.message = null;
            this.router.navigate(['/rooms'])
          }, 8000)
        }
        this.cdr.detectChanges();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error:(err) =>{
        this.showError(err?.error?.message || err?. message || "Unable to make a booking")
        this.cdr.detectChanges();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    })
  }

  cancelBookingPreview():void{
    this.showBookingPreview = false
  }

  get isLoading():boolean{
    return !this.room
  }
}