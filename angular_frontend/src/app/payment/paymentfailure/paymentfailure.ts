import { Component, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-paymentfailure',
  imports: [RouterLink],
  templateUrl: './paymentfailure.html',
  styleUrl: './paymentfailure.css',
})
export class Paymentfailure implements OnInit {
  bookingReference: string = '';
  errorReason: string = '';

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    // Lấy bookingReference từ path param (VD: /payment-failure/XYZ123)
    this.bookingReference = this.route.snapshot.paramMap.get('bookingReference') || '';
    
    // Lấy lý do lỗi từ query param (VD: ?reason=InsufficentFunds)
    this.route.queryParams.subscribe(params => {
      this.errorReason = params['reason'] || 'Unknown Error';
    });
  }
}