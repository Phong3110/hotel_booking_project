import { Component } from '@angular/core';
import { Api } from '../service/api';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  constructor(private router: Router, private api: Api){}

  get isAuthemticated():boolean{
    return this.api.isAuthenticated();
  }

  get isCustomer():boolean{
    return this.api.isCustomer();
  }

  get isAdmin():boolean{
    return this.api.isAdmin();
  }

  handleLogout(): void{
    const isLogout = window.confirm("Are you sure you want to logout? ")
    if (isLogout) {
      this.api.logout();
      this.router.navigate(['/home'])
    }
  }
}