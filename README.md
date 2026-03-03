# Hotel Booking Management System

A professional hotel booking management application designed to streamline reservations, guest management, and room administration.

## Features

- **Guest Management**: Register and manage guest information
- **Room Management**: Track room availability, types, and pricing
- **Booking System**: Create, modify, and cancel reservations
- **Availability Tracking**: Real-time room availability status
- **User Authentication**: Secure login and access control
- **Reporting**: Generate booking and occupancy reports

## Tech Stack

- **Backend**: Node.js, Express
- **Frontend**: React
- **Database**: MongoDB
- **Language**: JavaScript

## Project Structure

```
hotel_Booking_Management/
├── backend/          # Express backend API
├── frontend/         # React frontend app
├── README.md         # This file
```

## Getting Started

### Prerequisites

- Node.js >= 16.x
- npm >= 8.x
- MongoDB instance (local or cloud)

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd hotel_Booking_Management
   ```

2. Install backend dependencies:
   ```bash
   cd backend
   npm install
   ```

3. Install frontend dependencies:
   ```bash
   cd ../frontend
   npm install
   ```

4. Configure environment variables:

   - Copy `.env.example` to `.env` in the `backend` folder and update MongoDB URI and other secrets as needed.

5. Start the backend server:
   ```bash
   cd ../backend
   npm run dev
   ```

6. Start the frontend app (in a new terminal):
   ```bash
   cd frontend
   npm start
   ```

## Usage

### Example: Creating a Booking

Send a POST request to the backend API:

```bash
curl -X POST http://localhost:5000/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "guestId": "64a1f2c8e7b1c2a1b2c3d4e5",
    "roomId": "64a1f2c8e7b1c2a1b2c3d4f6",
    "checkIn": "2024-07-01",
    "checkOut": "2024-07-05"
  }'
```

### Example: Managing Rooms

Fetch all rooms via API:

```bash
curl http://localhost:5000/api/rooms
```

Or use the frontend UI to add/edit/delete rooms.

## API Endpoints / Features

| Endpoint                | Method | Description                  |
|-------------------------|--------|------------------------------|
| `/api/guests`           | GET    | List all guests              |
| `/api/guests`           | POST   | Register a new guest         |
| `/api/rooms`            | GET    | List all rooms               |
| `/api/rooms`            | POST   | Add a new room               |
| `/api/bookings`         | GET    | List all bookings            |
| `/api/bookings`         | POST   | Create a new booking         |
| `/api/auth/login`       | POST   | User login                   |

## Testing

Run backend tests:

```bash
cd backend
npm test
```

Run frontend tests:

```bash
cd frontend
npm test
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.

## Author

Phan Thiết Trung

## Support

For issues and questions, please [open an issue](../../issues) on the repository.

---

**Last Updated**: [Date]
