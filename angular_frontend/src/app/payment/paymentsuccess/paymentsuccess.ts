import { Component } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-paymentsuccess',
  imports: [RouterLink],
  templateUrl: './paymentsuccess.html',
  styleUrl: './paymentsuccess.css',
})
export class Paymentsuccess {
  bookingReference: string = '';

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.bookingReference = this.route.snapshot.paramMap.get('bookingReference') || '';
  }
}