# RESTful Resource Booking System API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.3-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%20%7C%20MySQL%20%7C%20H2-blue.svg)](https://www.postgresql.org/)
[![Swagger](https://img.shields.io/badge/OpenAPI%203-Swagger%20UI-green.svg)](http://localhost:8080/swagger-ui.html)
[![Tests](https://img.shields.io/badge/Tests-25%20Passed%20(100%25)-success.svg)]()

A production-grade, enterprise RESTful backend application built for the **Backend Developer Hiring Assessment**. This system provides secure resource management and conflict-free reservation scheduling with Role-Based Access Control (RBAC), stateless JWT authentication, decimal monetary precision, dynamic multi-field filtering, and comprehensive test coverage.

---

## 📑 Table of Contents
1. [Core Features & Requirements Checklist](#-core-features--requirements-checklist)
2. [Technology Stack](#-technology-stack)
3. [System Architecture & Design](#-system-architecture--design)
4. [Database & ER Model](#-database--er-model)
5. [Seed Credentials](#-seed-credentials)
6. [Quick Start & Setup Instructions](#-quick-start--setup-instructions)
7. [API Documentation & Swagger](#-api-documentation--swagger)
8. [API Endpoints Reference](#-api-endpoints-reference)
9. [Sample cURL Commands](#-sample-curl-commands)
10. [Running Automated Tests](#-running-automated-tests)

---

## 🎯 Core Features & Requirements Checklist

| # | Requirement | Implementation Status | Implementation Details |
|---|---|---|---|
| 1 | **JWT Login & Auth** |  Complete | `POST /auth/login` generates signed HMAC-SHA256 JWT tokens. Passwords hashed with BCrypt. |
| 2 | **RBAC (ADMIN & USER)** |  Complete | Enforced via Spring Security `SecurityFilterChain` and `@PreAuthorize` method annotations. |
| 3 | **Resource CRUD** |  Complete | ADMIN has full CRUD (`POST`, `GET`, `PUT`, `DELETE`). USER has read-only access. |
| 4 | **Reservation Ownership** |  Complete | USER identity is extracted **strictly from JWT SecurityContext**, never from the request body. |
| 5 | **Reservation Statuses** |  Complete | Strict lifecycle with `PENDING`, `CONFIRMED`, `CANCELLED` enum states. |
| 6 | **Decimal Pricing** |  Complete | Decimal storage (`DECIMAL(10,2)`) and dynamic calculation: `(Duration / 60) * pricePerHour`. |
| 7 | **Conflict / Overlap Detection** |  Complete | Prevents double-booking: rejects overlapping `PENDING` or `CONFIRMED` slots with `409 Conflict`. |
| 8 | **Dynamic Filtering** |  Complete | Spring Data JPA `Specification` predicates for `status`, `minPrice`, `maxPrice`, `resourceId`, date ranges. |
| 9 | **Pagination & Sorting** |  Complete | `page`, `size`, `sortBy`, `sortDirection` metadata wrapped in structured `PagedResponse<T>`. |
| 10 | **Data Isolation** |  Complete | Regular USER sees only their own reservations; ADMIN sees all reservations or can filter by user. |
| 11 | **Data Persistence** |  Complete | JPA / Hibernate with support for **PostgreSQL**, **MySQL**, and zero-config in-memory **H2**. |
| 12 | **Error Handling** |  Complete | `@RestControllerAdvice` global exception handler returns standardized JSON error envelopes. |
| 13 | **OpenAPI & Postman** |  Complete | Swagger UI at `/swagger-ui.html` + complete `Resource_Booking_System.postman_collection.json`. |
| 14 | **Automated Tests** |  Complete | 25 Unit & Integration tests covering Security, RBAC, CRUD, Overlap detection, and Filter logic. |

---

## 🛠 Technology Stack

- **Framework**: Spring Boot 3.3.4 (Java 21 / 17+)
- **Security**: Spring Security 6.3 + JJWT 0.12.6 (Stateless JWT Authentication)
- **Data & Persistence**: Spring Data JPA, Hibernate ORM 6.5
- **Databases**:
  - In-Memory H2 (Default for instant zero-dependency execution)
  - PostgreSQL 16 (Production Docker Profile)
  - MySQL 8.0 (Production Docker Profile)
- **Validation**: Jakarta Bean Validation (`@NotNull`, `@Future`, `@DecimalMin`, etc.)
- **Documentation**: SpringDoc OpenAPI 2.6.0 (Swagger UI)
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito, Spring Security Test, MockMvc, AssertJ

---

## 🏛 System Architecture & Design

```
                     HTTP Client / Postman / Swagger UI
                                    │
                         [Authorization: Bearer <token>]
                                    ▼
                     ┌─────────────────────────────┐
                     │   JwtAuthenticationFilter   │  ◄── Validates Signature & Expiration
                     └──────────────┬──────────────┘
                                    │ Populates SecurityContext
                                    ▼
                     ┌─────────────────────────────┐
                     │     SecurityFilterChain     │  ◄── Enforces URL & RBAC Rules
                     └──────────────┬──────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
   │ AuthController  │     │ResourceControl. │     │ReservationContr.│
   └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
            ▼                       ▼                       ▼
   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
   │   AuthService   │     │ ResourceService │     │ReservationServ. │
   └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
            │                       │                       │
            │                       │              [Overlap Conflict?]
            │                       │              [Price Calculation]
            │                       │              [Specification Predicates]
            ▼                       ▼                       ▼
   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
   │ UserRepository  │     │ResourceReposito.│     │ReservationRepo. │
   └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
            └───────────────────────┼───────────────────────┘
                                    ▼
                     ┌─────────────────────────────┐
                     │ PostgreSQL / MySQL / H2 DB  │
                     └─────────────────────────────┘
```

---

## 🗄 Database & ER Model

```
┌─────────────────────────┐              ┌─────────────────────────┐
│          USERS          │              │        RESOURCES        │
├─────────────────────────┤              ├─────────────────────────┤
│ PK  id         BIGINT   │              │ PK  id          BIGINT  │
│ UK  email      VARCHAR  │              │     name        VARCHAR │
│     password   VARCHAR  │              │     description TEXT    │
│     full_name  VARCHAR  │              │     type        VARCHAR │
│     role       VARCHAR  │              │     capacity    INTEGER │
│     created_at TIMESTAMP│              │     location    VARCHAR │
│     updated_at TIMESTAMP│              │     price_hour  DECIMAL │
└───────────┬─────────────┘              │     active      BOOLEAN │
            │ 1                          │     created_at  TIMESTAMP│
            │                            │     updated_at  TIMESTAMP│
            │ N                          └───────────┬─────────────┘
      ┌─────┴────────────────────────────────────────┴────┐
      │                   RESERVATIONS                    │
      ├───────────────────────────────────────────────────┤
      │ PK  id           BIGINT                           │
      │ FK  user_id      BIGINT  (NOT NULL)               │
      │ FK  resource_id  BIGINT  (NOT NULL)               │
      │     start_time   TIMESTAMP (NOT NULL)             │
      │     end_time     TIMESTAMP (NOT NULL)             │
      │     status       VARCHAR (PENDING/CONFIRMED/CANCEL│
      │     total_price  DECIMAL(10,2) (NOT NULL)         │
      │     notes        TEXT                             │
      │     created_at   TIMESTAMP (NOT NULL)             │
      │     updated_at   TIMESTAMP                        │
      └───────────────────────────────────────────────────┘
```

---

## 🔑 Seed Credentials

The system automatically initializes test accounts upon startup (`DataSeeder.java`):

| Role | Email | Password | Permissions |
|---|---|---|---|
| **ADMIN** | `admin@example.com` | `Admin@123` | Full CRUD on resources, view all reservations, update status, delete reservations. |
| **USER 1** | `user@example.com` | `User@123` | Read-only resources, create reservations, view & cancel own reservations. |
| **USER 2** | `user2@example.com` | `User2@123` | Second user for testing RBAC data segregation and isolation. |

---

## 🚀 Quick Start & Setup Instructions

### Option 1: Run Locally with Default Profile (Zero Configuration)
The application defaults to an embedded H2 database for instant local execution.

```bash
# 1. Clone the repository
cd resource-booking-system

# 2. Build and run with Maven
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

---

### Option 2: Run with Docker Compose (PostgreSQL)
Run the application along with a dedicated PostgreSQL database container:

```bash
docker-compose up --build
```

---

### Option 3: Run with MySQL Profile
To run with a local MySQL instance:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
*(Configure environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` if custom).*

---

## 📖 API Documentation & Swagger

Interactive Swagger / OpenAPI 3 documentation is available when the application is running:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:bookingdb`, Username: `sa`, Password: *blank*)

---

## 📡 API Endpoints Reference

### 1. Authentication Endpoints (`/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/auth/login` | Public | Authenticates credentials and returns JWT Bearer token |
| `POST` | `/auth/register` | Public | Registers a new user account |
| `GET` | `/auth/me` | Authenticated | Fetches profile of currently logged-in user |

### 2. Resource Management Endpoints (`/resources`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/resources` | USER / ADMIN | Get paginated list of resources (`page`, `size`, `sortBy`, `sortDirection`) |
| `GET` | `/resources/{id}` | USER / ADMIN | Get details of a specific resource |
| `POST` | `/resources` | ADMIN Only | Create a new bookable resource |
| `PUT` | `/resources/{id}` | ADMIN Only | Update an existing resource |
| `DELETE` | `/resources/{id}` | ADMIN Only | Delete a resource |

### 3. Reservation Management Endpoints (`/reservations`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/reservations` | USER / ADMIN | Create reservation (**User identity extracted from JWT**) |
| `GET` | `/reservations` | USER / ADMIN | Get reservations. **USER sees own; ADMIN sees all**. Supports filters & pagination |
| `GET` | `/reservations/{id}` | USER / ADMIN | Get reservation by ID (**USER restricted to own**) |
| `PUT` | `/reservations/{id}` | USER / ADMIN | Update reservation times and notes |
| `PATCH` | `/reservations/{id}/status` | ADMIN Only | Update status (`PENDING`, `CONFIRMED`, `CANCELLED`) |
| `PATCH` | `/reservations/{id}/cancel` | USER / ADMIN | Cancel reservation (**USER can cancel own**) |
| `DELETE` | `/reservations/{id}` | ADMIN Only | Delete reservation |

---

## 💻 Sample cURL Commands

### 1. Login as Admin
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "Admin@123"}'
```

### 2. Login as User
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "User@123"}'
```

### 3. Create Resource (ADMIN Only)
```bash
curl -X POST http://localhost:8080/resources \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Design Thinking Studio",
    "description": "Collaborative workshop space with digital whiteboards",
    "type": "STUDIO",
    "capacity": 15,
    "location": "Building 4, Room 201",
    "pricePerHour": 95.00,
    "active": true
  }'
```

### 4. Create Reservation (USER)
*Note: `userId` is not required or accepted in the body; it is extracted automatically from the Bearer token.*
```bash
curl -X POST http://localhost:8080/reservations \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-09-10T10:00:00",
    "endTime": "2026-09-10T12:00:00",
    "notes": "Sprint planning session"
  }'
```

### 5. Filter Reservations by Status and Price
```bash
curl -X GET "http://localhost:8080/reservations?status=CONFIRMED&minPrice=50.00&maxPrice=300.00&page=0&size=10&sortBy=totalPrice&sortDirection=desc" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

## 🧪 Running Automated Tests

Execute the comprehensive test suite (25 Unit & Integration tests):

```bash
mvn clean test
```

### Test Summary:
- **`AuthControllerIntegrationTest`**: Validates login token generation, password encryption, 401 unauthorized rejection, registration constraints.
- **`ResourceControllerIntegrationTest`**: Validates RBAC permissions (Admin create/update/delete allowed vs User 403 Forbidden).
- **`ReservationControllerIntegrationTest`**: Validates JWT user extraction, data ownership isolation, filtering, pagination, sorting, and 409 conflict collision detection.
- **`ReservationServiceTest`**: Mockito unit tests verifying price calculation algorithms, date range validation, and collision logic.
- **`JwtServiceTest`**: Tests JWT signing, claims extraction, and token expiration.
