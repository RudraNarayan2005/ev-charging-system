# EV Charging Station Booking System

A production-ready REST API backend for booking EV charging station slots, built with Java Spring Boot.

## Tech Stack
- Java 17
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL 8
- Swagger UI (OpenAPI 3)
- Lombok

## Features
- User registration and login with JWT authentication
- Role-based access control (USER, ADMIN, STATION_OPERATOR)
- Search charging stations by city, connector type, price
- Geo-proximity search using Haversine formula
- Real-time slot availability check for any time window
- Concurrent booking conflict prevention
- Booking creation, cancellation, and history
- Auto-complete expired bookings via scheduled jobs
- Paginated responses for all list endpoints
- Swagger UI for API testing

## Project Structure
```
src/main/java/com/bank/ev_charging_system/
├── config/         SecurityConfig.java
├── controller/     AuthController, BookingController, StationController, UserController
├── dto/            Request and Response DTOs
├── entity/         User, ChargingStation, ChargingSlot, Booking
├── exception/      GlobalExceptionHandler, Custom Exceptions
├── repository/     JPA Repositories
├── service/        Business Logic Services
└── util/           JwtUtil, BookingReferenceGenerator
```

## Setup and Run

### Step 1 - Create Database
```sql
CREATE DATABASE ev_charging_db;
```

### Step 2 - Configure Database
Create `src/main/resources/application-mysql.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ev_charging_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

### Step 3 - Run
```bash
mvn spring-boot:run
```

### Step 4 - Open Swagger UI
```
http://localhost:8080/api/v1/swagger-ui.html
```

## API Endpoints

### Auth
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /auth/register | Register new user | No |
| POST | /auth/login | Login and get JWT | No |

### Stations
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /stations | List all stations | No |
| GET | /stations/{id} | Get station details | No |
| GET | /stations/search | Search stations | No |
| GET | /stations/nearby | Find nearby stations | No |
| GET | /stations/{id}/availability | Check availability | No |
| POST | /stations | Create station | Admin |
| POST | /stations/{id}/slots | Add slot | Admin |
| PATCH | /stations/{id}/status | Update status | Admin |

### Bookings
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /bookings | Create booking | User |
| GET | /bookings/my | Get my bookings | User |
| GET | /bookings/{id} | Get booking by ID | User |
| GET | /bookings/reference/{ref} | Get by reference | User |
| PATCH | /bookings/{id}/cancel | Cancel booking | User |
| GET | /bookings/station/{id} | Station bookings | Admin |

### Users
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /users/me | Get my profile | User |
| PUT | /users/me | Update profile | User |

## Concurrency Handling
Double bookings are prevented using three layers:
1. Service layer checks for conflicting bookings before inserting
2. SERIALIZABLE transaction isolation prevents phantom reads
3. Optimistic locking with @Version prevents lost updates

## Sample Response
```json
{
  "success": true,
  "message": "Booking confirmed!",
  "data": {
    "bookingReference": "EVC-202603151200-AB12CD",
    "stationName": "Kolkata EV Hub",
    "slotNumber": "SLOT-A1",
    "startTime": "2026-04-01T10:00:00",
    "endTime": "2026-04-01T12:00:00",
    "totalAmount": 160.00,
    "status": "CONFIRMED"
  },
  "timestamp": "2026-03-15T00:07:33"
}
```

## Author
Built with Spring Boot - EV Charging Station Booking System
