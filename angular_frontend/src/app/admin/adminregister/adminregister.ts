import { Component, ChangeDetectorRef } from '@angular/core';
import { Api } from '../../service/api';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';

@Component({
  selector: 'app-adminregister',
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './adminregister.html',
  styleUrl: './adminregister.css',
})
export class Adminregister {
  constructor(private api: Api, private router: Router, private cdr: ChangeDetectorRef) {}

  formData: any = {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    password: '',
    role: '',
  };

  message: string | null = null;

  handleSubmit() {
    if (
      !this.formData.email ||
      !this.formData.firstName ||
      !this.formData.lastName ||
      !this.formData.phoneNumber ||
      !this.formData.password ||
      !this.formData.role
    ) {
      this.showError('All fields are required');
      return;
    }

    this.api.registerUser(this.formData).subscribe({
      next: (res) => {
        this.router.navigate(['/admin']);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.showError(
          err?.error?.message ||
            err.message ||
            'Unable To Register a user: ' + err
        );
        this.cdr.detectChanges();
      },
    });
  }

  showError(msg: string) {
    this.message = msg;
    setTimeout(() => {
      this.message = null;
    }, 4000);
  }
}