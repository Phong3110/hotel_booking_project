import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import CryptoJS from 'crypto-js';

interface PaymentRequest {
  bookingReference: string;
  amount: number;
  transactionId?: string;
  success?: boolean;
  failureReason?: string;
}

interface PaymentResponse {
  status: string;
  message: string;
}

interface BookingStatusResponse {
  status: string;
  message: string;
  amount: number;
  bookingReference: string;
}

@Injectable({
  providedIn: 'root',
})
export class Api {
  private static BASE_URL = 'http://localhost:8080/api';
  private static ENCRYPTION_KEY = 'phong-encrypt-key';

  constructor(private http: HttpClient) {}

  // ==========================================
  // ENCRYPTION HELPERS
  // ==========================================
  encryptAndSaveToStorage(key: string, value: string): void {
    const encryptedValue = CryptoJS.AES.encrypt(
      value,
      Api.ENCRYPTION_KEY
    ).toString();
    localStorage.setItem(key, encryptedValue);
  }

  // Retrieve from localStorage and decrypt
  private getFromStorageAndDecrypt(key: string): string | null {
    try {
      const encryptedValue = localStorage.getItem(key);
      if (!encryptedValue) return null;
      return CryptoJS.AES.decrypt(
        encryptedValue,
        Api.ENCRYPTION_KEY
      ).toString(CryptoJS.enc.Utf8);
    } catch (error) {
      return null;
    }
  }
  
  //clear authentication data
  private clearAuth(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
  }

  private getHeader(): HttpHeaders {
    const token = this.getFromStorageAndDecrypt('token');
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });
  }

  // ==========================================
  // AUTH API METHODS
  // ==========================================
  registerUser(body: any): Observable<any> {
    return this.http.post(`${Api.BASE_URL}/auth/register`, body);
  }

  loginUser(body: any): Observable<any> {
    return this.http.post(`${Api.BASE_URL}/auth/login`, body);
  }

  logout(): void {
    this.clearAuth();
  }

  // ==========================================
  // AUTHENTICATION CHECKERS
  // ==========================================
  isAuthenticated(): boolean {
    const token = this.getFromStorageAndDecrypt('token');
    return !!token;
  }

  isAdmin(): boolean {
    const role = this.getFromStorageAndDecrypt('role');
    return role === 'ADMIN';
  }

  isCustomer(): boolean {
    const role = this.getFromStorageAndDecrypt('role');
    return role === 'CUSTOMER';
  }

  // ==========================================
  // USERS API METHODS
  // ==========================================
  myProfile(): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/users/account`, {
      headers: this.getHeader(),
    });
  }

  myBookings(): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/users/bookings`, {
      headers: this.getHeader(),
    });
  }

  deleteAccount(): Observable<any> {
    return this.http.delete(`${Api.BASE_URL}/users/delete`, {
      headers: this.getHeader(),
    });
  }

  // ==========================================
  // ROOMS API METHODS
  // ==========================================
  addRoom(formData: any): Observable<any> {
    return this.http.post(`${Api.BASE_URL}/rooms/add`, formData, {
      headers: this.getHeader(),
    });
  }

  updateRoom(formData: any): Observable<any> {
    return this.http.put(`${Api.BASE_URL}/rooms/update`, formData, {
      headers: this.getHeader(),
    });
  }

  getAvailableRooms(
    checkInDate: string,
    checkOutDate: string,
    roomType: string
  ): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/rooms/available`, {
      params: { checkInDate, checkOutDate, roomType },
    });
  }

  getRoomTypes(): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/rooms/types`);
  }

  getAllRooms(): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/rooms/all`);
  }

  getRoomById(roomId: string): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/rooms/${roomId}`);
  }

  deleteRoom(roomId: string): Observable<any> {
    return this.http.delete(`${Api.BASE_URL}/rooms/delete/${roomId}`, {
      headers: this.getHeader(),
    });
  }

  // ==========================================
  // BOOKINGS API METHODS
  // ==========================================
  bookRoom(booking: any): Observable<any> {
    return this.http.post(`${Api.BASE_URL}/bookings`, booking, {
      headers: this.getHeader(),
    });
  }

  getAllBookings(): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/bookings/all`, {
      headers: this.getHeader(),
    });
  }

  updateBooking(booking: any): Observable<any> {
    return this.http.put(`${Api.BASE_URL}/bookings/update`, booking, {
      headers: this.getHeader(),
    });
  }

  getBookingByReference(bookingCode: string): Observable<any> {
    return this.http.get(`${Api.BASE_URL}/bookings/${bookingCode}`, {
      headers: this.getHeader(),
    });
  }

  // SỬA: Check booking status với token từ URL
  checkBookingStatus(token: string): Observable<BookingStatusResponse> {
    return this.http.get<BookingStatusResponse>(
      `${Api.BASE_URL}/bookings/status`,
      {
        params: { token }, // Chỉ cần token
        headers: this.getHeader(),
      }
    );
  }

  // THÊM: Cancel booking
  cancelBooking(bookingReference: string): Observable<any> {
    return this.http.delete(
      `${Api.BASE_URL}/bookings/cancel/${bookingReference}`,
      {
        headers: this.getHeader(),
      }
    );
  }

  // ==========================================
  // STRIPE PAYMENT METHODS
  // ==========================================

  // SỬA: Thêm token parameter
  createStripePaymentIntent(
    token: string,
    paymentRequest: PaymentRequest
  ): Observable<string> {
    return this.http.post<string>(
      `${Api.BASE_URL}/stripe/pay`,
      paymentRequest,
      {
        params: { token }, // Thêm token vào query params
        headers: this.getHeader(),
      }
    );
  }

  // SỬA: Thêm token parameter
  updateStripeBooking(
    token: string,
    paymentRequest: PaymentRequest
  ): Observable<void> {
    return this.http.put<void>(
      `${Api.BASE_URL}/stripe/update`,
      paymentRequest,
      {
        params: { token }, // Thêm token vào query params
        headers: this.getHeader(),
      }
    );
  }

  // ==========================================
  // PAYPAL PAYMENT METHODS
  // ==========================================

  // SỬA: Thêm token parameter
  createPaypalOrder(
    token: string,
    paymentRequest: PaymentRequest
  ): Observable<{ orderId: string }> {
    return this.http.post<{ orderId: string }>(
      `${Api.BASE_URL}/paypal/create`,
      paymentRequest,
      {
        params: { token }, // Thêm token vào query params
        headers: this.getHeader(),
      }
    );
  }

  // SỬA: Thêm token parameter
  capturePaypalOrder(
    token: string,
    orderId: string
  ): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${Api.BASE_URL}/paypal/capture`,
      {}, // Body trống
      {
        params: { token, orderId }, // Token + orderId
        headers: this.getHeader(),
      }
    );
  }
}
