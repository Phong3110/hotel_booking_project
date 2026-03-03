# 🏨 Hotel Booking System

A full-stack hotel room booking web application built with **Spring Boot** and **Angular**, featuring secure authentication, real-time room availability, and integrated payment processing.

---

## ✨ Features

### 👤 User
- Register & login with JWT authentication
- Search available rooms by date and type
- View room details and hotel services
- Create bookings with guest information
- Pay via **Stripe** (Credit Card) or **PayPal**
- Receive email confirmation & payment notifications
- View and cancel personal bookings
- Manage account profile

### 🔐 Admin
- Add, update, and delete rooms
- View and manage all bookings
- Update booking status (Check-in / Check-out)
- View all registered users

---

## 🛠️ Tech Stack

### Backend
| Technology | Description |
|---|---|
| Java 17 + Spring Boot 3 | Core backend framework |
| Spring Security + JWT | Authentication & authorization |
| Spring Data JPA | Database ORM |
| MySQL | Relational database |
| Stripe SDK | Credit card payment processing |
| PayPal SDK | PayPal payment processing |
| JavaMailSender | Email notifications |
| Swagger / OpenAPI 3 | API documentation |
| Lombok | Boilerplate reduction |
| ModelMapper | DTO mapping |

### Frontend
| Technology | Description |
|---|---|
| Angular 17+ | SPA framework |
| Tailwind CSS | Utility-first styling |
| Stripe.js | Frontend card payment UI |
| PayPal JS SDK | PayPal button integration |
| RxJS | Reactive data handling |

---

## 🗂️ Project Structure

```
hotel_booking_project/
├── spring_backend/          # Spring Boot REST API
│   └── src/main/java/com/example/HotelBooking/
│       ├── controllers/     # REST endpoints
│       ├── services/        # Business logic
│       ├── entities/        # JPA entities
│       ├── repositories/    # Database access
│       ├── dtos/            # Data transfer objects
│       ├── security/        # JWT & Spring Security
│       ├── payments/        # Stripe & PayPal integration
│       └── config/          # App configuration
│
└── angular_frontend/        # Angular SPA
    └── src/app/
        ├── home/            # Landing page & room search
        ├── rooms/           # Room listing & details
        ├── booking/         # Booking flow
        ├── payment/         # Payment page (Stripe/PayPal)
        ├── admin/           # Admin dashboard
        ├── service/         # API service layer
        └── auth/            # Login & Register
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 18+ & npm
- MySQL 8+
- Angular CLI (`npm install -g @angular/cli`)

---

### ⚙️ Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/hotel_booking_project.git
   cd hotel_booking_project/spring_backend
   ```

2. **Configure `application.properties`**
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/hotel_booking_db
   spring.datasource.username=your_mysql_username
   spring.datasource.password=your_mysql_password
   spring.jpa.hibernate.ddl-auto=update

   jwt.secret=your_jwt_secret_key
   jwt.expiration=15552000000

   # Email (e.g. Gmail SMTP)
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your_email@gmail.com
   spring.mail.password=your_app_password

   # Stripe
   stripe.secret.key=sk_test_your_stripe_secret_key

   # PayPal
   paypal.client.id=your_paypal_client_id
   paypal.client.secret=your_paypal_client_secret
   ```

3. **Run the backend**
   ```bash
   ./mvnw spring-boot:run
   ```
   The API will be available at `http://localhost:8080`

---

### 🌐 Frontend Setup

1. **Navigate to the frontend folder**
   ```bash
   cd ../angular_frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   ng serve
   ```
   The app will be available at `http://localhost:4200`

---

## 📡 API Endpoints

### Public
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT |
| GET | `/api/rooms/all` | Get all rooms |
| GET | `/api/rooms/available` | Search available rooms |
| GET | `/api/rooms/{id}` | Get room details |
| GET | `/api/bookings/status` | Check payment link status |

### Authenticated Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bookings` | Create a booking |
| GET | `/api/bookings/{reference}` | View booking |
| DELETE | `/api/bookings/cancel/{ref}` | Cancel booking |
| GET | `/api/users/account` | Get own profile |
| POST | `/api/stripe/pay` | Stripe payment intent |
| POST | `/api/paypal/create` | Create PayPal order |
| POST | `/api/paypal/capture` | Capture PayPal payment |

### Admin Only
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/rooms/add` | Add new room |
| PUT | `/api/rooms/update` | Update room |
| DELETE | `/api/rooms/delete/{id}` | Delete room |
| GET | `/api/bookings/all` | View all bookings |
| PUT | `/api/bookings/update` | Update booking status |
| GET | `/api/users/all` | View all users |

> 📖 Full API documentation available at `http://localhost:8080/swagger-ui/index.html`

---

## 💳 Payment Flow

1. User creates a booking → system generates a **secure one-time payment link** (expires in 10 minutes)
2. A confirmation email is sent with the payment link
3. User selects **Stripe** or **PayPal** to complete payment
4. On success/failure, booking status is updated and another email notification is sent

---

## 🔒 Security

- **JWT-based stateless authentication** (6-month token expiry)
- Role-based access control: `ADMIN` and `CUSTOMER`
- Payment links are **single-use tokens** with expiry to prevent fraud
- Idempotency check on transactions to prevent duplicate charges
- BCrypt password hashing

---

## 🧪 Booking Validation Rules

- Maximum booking duration: **30 nights**
- Cannot book more than **1 year** in advance
- Maximum **3 pending (unpaid) bookings** per user at a time
- Cannot cancel a booking that is already **checked in**
- Cannot check in **before check-in date** or without **completed payment**

---

## 📄 License

This project is for educational purposes. Feel free to fork and adapt it.

---

## 📬 Contact

For questions or support, reach out at: **phong0818689834@gmail.com**
