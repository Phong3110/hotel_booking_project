import { Component, ChangeDetectorRef } from '@angular/core';
import { Api } from '../service/api';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  constructor( private api: Api, private router: Router, private cdr: ChangeDetectorRef){}
  
  formData: any = {

    email: '',
    password: ''

  }
  error: any = null;

  async handleSubmit(){

    console.log("hanlde submit is called for login")

    if (!this.formData.email || !this.formData.password) {
      this.showError("Please fill all the fields correctly")
      return
    }

    this.api.loginUser(this.formData).subscribe({
      next: (res:any) => {
        if (res.status === 200) {
          this.api.encryptAndSaveToStorage('token', res.token);
          this.api.encryptAndSaveToStorage('role', res.role);
          this.router.navigate(['/home'])
        }
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.showError(err?.error?.message || err.message || 'Unable To Login: ' + err)
        this.cdr.detectChanges();
      }
    });
  }

  showError(msg: string){
    this.error = msg;
    setTimeout(()=> {
      this.error = null
    }, 4000);
  }

}
