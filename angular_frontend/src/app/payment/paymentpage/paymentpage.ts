import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { loadStripe, Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';
import { Api } from '../../service/api';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { lastValueFrom } from 'rxjs';

declare var paypal: any;

@Component({
  selector: 'app-paymentpage',
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './paymentpage.html',
  styleUrl: './paymentpage.css',
})
export class Paymentpage implements OnInit {
  // --- STRIPE VARS ---
  stripe: Stripe | null = null;
  elements: StripeElements | null = null;
  cardElement: StripeCardElement | null = null;

  // --- COMMON VARS ---
  error: any = null;
  processing: boolean = false;

  // --- TOKEN-BASED VARS ---
  token: string = ''; // Token từ URL
  bookingReference: string = '';
  amount: number = 0;
  paymentReady: boolean = false;
  loadingStatus: boolean = true;

  // --- PAYMENT METHOD SELECTION ---
  paymentMethod: 'stripe' | 'paypal' = 'stripe';
  paypalRendered: boolean = false;

  constructor(
    private api: Api,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}
  
  async ngOnInit() {
    // 1. Lấy token từ query param
    this.route.queryParams.subscribe(async params => {
      this.token = params['token'];
      
      if (!this.token) {
        this.showError('Invalid payment link. Missing token.');
        return;
      }

      // 2. Validate token và lấy booking info
      await this.validateToken();
    });
  }

  async validateToken() {
    this.loadingStatus = true;
    
    try {
      const response = await lastValueFrom(this.api.checkBookingStatus(this.token));
      
      // Backend trả về: { status: 'OK', message: '...', amount: 400 }
      if (response.status === 'OK') {
        this.amount = response.amount || 0;
        // Backend cũng nên trả bookingReference, nếu không có thì phải parse từ token
        // Giả sử backend trả về bookingReference trong response
        this.bookingReference = (response as any).bookingReference || '';
        
        this.paymentReady = true;
        this.loadingStatus = false;
        
        // Initialize Stripe sau khi validate thành công
        await this.initializeStripe();
        this.cdr.detectChanges();
        
      } else {
        this.showError(response.message || 'Invalid payment link.');
      }
      
    } catch (err: any) {
      this.loadingStatus = false;
      console.error('Token validation failed:', err);
      
      let errorMsg = 'Payment link validation failed.';
      if (err.error?.message) {
        errorMsg = err.error.message;
      } else if (err.status === 404) {
        errorMsg = 'Payment link not found or expired.';
      }
      
      this.showError(errorMsg);
    }
  }

  // --- LOGIC CHUYỂN ĐỔI TAB ---
  onMethodChange() {
    this.error = null;
    
    if (this.paymentMethod === 'paypal' && !this.paypalRendered) {
      setTimeout(() => {
        this.renderPaypalButton();
      }, 100);
    } else if (this.paymentMethod === 'stripe') {
      setTimeout(() => {
        if (this.cardElement) {
          this.cardElement.unmount();
          this.cardElement.mount('#card-element');
        }
      }, 100);
    }
  }

  // ==========================================
  // STRIPE LOGIC
  // ==========================================
  async initializeStripe() {
    this.stripe = await loadStripe('pk_test_51SWZt5JCHCmyWFVAJcpdipbigY1ZhgtLDvfJS1aslcOf0igXLrbNTstSBa57b9gHCQRo313WEw30CCsTJN5OI2Cq00wYYCewKJ');
    
    if (!this.stripe) {
      console.error('Stripe failed to load');
      return;
    }

    this.elements = this.stripe.elements();
    this.cardElement = this.elements.create('card');
    
    // Đợi một chút để đảm bảo DOM đã sẵn sàng
    setTimeout(() => {
      const cardDiv = document.getElementById('card-element');
      if (cardDiv && this.cardElement) {
        this.cardElement.mount('#card-element');
        this.cdr.detectChanges();
      }
    }, 100);
  }

  async handleStripeSubmit(e: Event) {
    e.preventDefault();
    if (!this.stripe || !this.elements || !this.cardElement) return;

    this.processing = true;

    const paymentRequest = {
      bookingReference: this.bookingReference,
      amount: this.amount,
    };

    // Gọi API với token
    this.api.createStripePaymentIntent(this.token, paymentRequest).subscribe({
      next: async (response: any) => {
        // Extract clientSecret từ response
        const clientSecret = typeof response === 'string' 
          ? response 
          : response.clientSecret;
        
        if (!clientSecret) {
          this.processing = false;
          this.showError('Invalid response from server');
          return;
        }

        const result = await this.stripe!.confirmCardPayment(clientSecret, {
          payment_method: { card: this.cardElement! },
        });

        if (result.error) {
          this.processing = false;
          const msg = result.error.message || 'Payment Failed';
          this.showError(msg);
          
          // Update booking với token
          this.updateBookingStripe('failed', '', msg);
          
        } else if (result.paymentIntent?.status === 'succeeded') {
          this.processing = false;
          
          // Update booking với token
          this.updateBookingStripe('succeeded', result.paymentIntent.id, '');
          this.showSuccess();
        }
        
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.processing = false;
        console.error('Error creating payment intent:', error);
        this.showError('Error creating payment intent');
        this.cdr.detectChanges();
      }
    });
  }

  updateBookingStripe(status: string, transactionId: string, reason: string) {
    const body = {
      bookingReference: this.bookingReference,
      amount: this.amount,
      transactionId: transactionId,
      success: status === 'succeeded',
      failureReason: reason
    };

    // Gọi với token
    this.api.updateStripeBooking(this.token, body).subscribe({
      next: () => console.log('Booking updated'),
      error: (err) => console.error('Update booking error:', err)
    });
  }

  // ==========================================
  // PAYPAL LOGIC
  // ==========================================
  renderPaypalButton() {
    if (!paypal) {
      this.showError('PayPal SDK not loaded. Check index.html');
      return;
    }

    const container = document.getElementById('paypal-button-container');
    if (container) container.innerHTML = '';

    paypal.Buttons({
      style: {
        layout: 'vertical',
        color: 'gold',
        shape: 'rect',
        label: 'paypal'
      },

      createOrder: async (data: any, actions: any) => {
        const body = {
          bookingReference: this.bookingReference,
          amount: this.amount
        };
        
        try {
          // Gọi với token
          const res: any = await lastValueFrom(
            this.api.createPaypalOrder(this.token, body)
          );
          return res.orderId;
        } catch (err) {
          console.error('Error creating PayPal order:', err);
          this.showError('Could not initiate PayPal payment');
          throw err;
        }
      },

      onApprove: async (data: any, actions: any) => {
        this.processing = true;
        const orderId = data.orderID;

        try {
          // Gọi với token
          await lastValueFrom(
            this.api.capturePaypalOrder(this.token, orderId)
          );
          this.processing = false;
          this.showSuccess();
        } catch (err) {
          this.processing = false;
          console.error('Capture error:', err);
          this.showError('Payment capture failed. Please contact support.');
        }
      },

      onError: (err: any) => {
        console.error('PayPal Error:', err);
        this.showError('An error occurred with PayPal.');
      }
    }).render('#paypal-button-container');
    
    this.paypalRendered = true;
  }

  // ==========================================
  // HELPER METHODS
  // ==========================================
  showError(msg: string) {
    this.router.navigate(['/payment-failure', this.bookingReference], {
      queryParams: { reason: msg }
    });
  }

  showSuccess() {
    this.router.navigate(['/payment-success', this.bookingReference]);
  }
}