import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Register } from './register/register';
import { Profile } from './profile/profile';
import { Guard } from './service/guard';
import { Editprofile } from './editprofile/editprofile';
import { Home } from './home/home';
import { Rooms } from './rooms/rooms';
import { Roomdetails } from './roomdetails/roomdetails';
import { Findbooking } from './findbooking/findbooking';
import { Paymentpage } from './payment/paymentpage/paymentpage';
import { Paymentsuccess } from './payment/paymentsuccess/paymentsuccess';
import { Paymentfailure } from './payment/paymentfailure/paymentfailure';
import { Adminhome } from './admin/adminhome/adminhome';
import { Managerooms } from './admin/managerooms/managerooms';
import { Addroom } from './admin/addroom/addroom';
import { Editroom } from './admin/editroom/editroom';
import { Managebookings } from './admin/managebookings/managebookings';
import { Updatebooking } from './admin/updatebooking/updatebooking';
import { Adminregister } from './admin/adminregister/adminregister';

export const routes: Routes = [
    {path: 'login', component: Login},
    {path: 'register', component: Register},
    {path: 'home', component: Home},

    {path: 'rooms', component: Rooms},
    {path: 'room-details/:id', component: Roomdetails, canActivate: [Guard]},
    {path: 'find-booking', component: Findbooking},


    {path: 'profile', component: Profile, canActivate: [Guard]},
    {path: 'edit-profile', component: Editprofile, canActivate: [Guard]},

    // SỬA: PAYMENT ROUTES - Dùng query param thay vì path param
    {path: 'payment', component: Paymentpage}, // KHÔNG cần Guard vì email link public
    {path: 'payment-success/:bookingReference', component: Paymentsuccess},
    {path: 'payment-failure/:bookingReference', component: Paymentfailure}, // Fix typo: failue → failure

    //ADMIN ROUTES OR PAGES

    {path: 'admin', component: Adminhome, canActivate: [Guard], data: {requiresAdmin: true}},
    {path: 'admin/manage-rooms', component: Managerooms, canActivate: [Guard], data: {requiresAdmin: true}},
    {path: 'admin/add-room', component: Addroom, canActivate: [Guard], data: {requiresAdmin: true}},
    {path: 'admin/edit-room/:id', component: Editroom, canActivate: [Guard], data: {requiresAdmin: true}},
    {path: 'admin/manage-bookings', component: Managebookings, canActivate: [Guard], data: {requiresAdmin: true}},
    {path: 'admin/edit-booking/:bookingCode', component: Updatebooking, canActivate: [Guard], data: {requiresAdmin: true}},
    {path: 'admin/admin-register', component: Adminregister, canActivate: [Guard], data: {requiresAdmin: true}},

    {path: '**', redirectTo: 'home'}
];
