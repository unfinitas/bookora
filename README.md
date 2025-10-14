# Bookora

Modern booking system for personal services

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Data access layer
- **PostgreSQL** - Primary database
- **Flyway** - Database migrations
- **MapStruct** - Object mapping
- **Lombok** - Boilerplate code reduction
- **Hypersistence Utils** - Hibernate utilities
- **JWT (JJWT)** - Token-based authentication
- **Springdoc OpenAPI** - API documentation
- **Maven** - Build tool

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 17

## Database Schema (ERD)

```mermaid
erDiagram
    t_user ||--o| t_provider : "has"
    t_user ||--o{ t_booking : "makes"
    t_provider ||--o{ t_service : "offers"
    t_provider ||--o{ t_booking : "receives"
    t_provider ||--o{ t_availability : "has"
    t_provider }o--o| t_address : "located_at"
    t_service ||--o{ t_booking : "booked_in"
    t_booking ||--|| t_guest_access_token : "has"

    t_user {
        uuid id PK
        string full_name
        string email UK "uq_user_email"
        string password
        string role "GUEST, CUSTOMER, PROVIDER"
        boolean is_guest
        string phone_number
        timestamp created_at
        timestamp updated_at
    }

    t_provider {
        uuid id PK
        uuid user_id FK,UK "uq_provider_user_id"
        bigint address_id FK
        string business_name
        text description
        boolean is_verified
        timestamp created_at
        timestamp updated_at
    }

    t_address {
        bigint id PK
        string street
        string city
        string state_province
        string zip_code
        string country
        timestamp created_at
        timestamp updated_at
    }

    t_service {
        bigint id PK
        uuid provider_id FK
        string name
        text description
        integer duration_minutes
        decimal price
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }

    t_booking {
        bigint id PK
        uuid customer_id FK
        uuid provider_id FK
        bigint service_id FK
        timestamp start_time
        timestamp end_time
        string status "PENDING, CONFIRMED, CANCELLED, COMPLETED"
        text notes
        timestamp created_at
        timestamp updated_at
    }

    t_availability {
        bigint id PK
        uuid provider_id FK
        date availability_date
        time start_time
        time end_time
        boolean is_available
        timestamp created_at
        timestamp updated_at
    }

    t_guest_access_token {
        bigint id PK
        bigint booking_id FK,UK "uq_guest_access_token_booking_id"
        uuid token UK "uq_guest_access_token_token"
        timestamp expires_at
        timestamp used_at
        timestamp created_at
        timestamp updated_at
    }
```

### Key Relationships

- **User-Provider**: One-to-one relationship. A user can become a provider.
- **User-Booking**: One-to-many. A customer (user) can make multiple bookings.
- **Provider-Service**: One-to-many. A provider offers multiple services.
- **Provider-Booking**: One-to-many. A provider receives multiple bookings.
- **Provider-Availability**: One-to-many. A provider defines multiple availability time.
- **Provider-Address**: Many-to-one. Multiple providers can share the same address (co-working spaces, business centers).
- **Service-Booking**: One-to-many. A service can be booked multiple times.
- **Booking-GuestAccessToken**: One-to-one. Each booking can have a guest access token.

### Notes

- **Address**: Separate entity table for reusability, historical tracking, and efficient querying
  - Multiple providers can share the same address (co-working spaces, business centers)
  - Enables address history tracking via BaseEntity timestamps
  - Supports efficient location-based queries with indexed city/country columns
- **UserRole**: Enum (GUEST, CUSTOMER, PROVIDER)
- **BookingStatus**: Enum (PENDING, CONFIRMED, CANCELLED, COMPLETED)
- **Naming Conventions**:
  - Tables: `t_<entity>` prefix
  - Columns: `snake_case`
  - Boolean columns: `is_<name>` prefix
  - Timestamp columns: `<action>_at` suffix
  - Unique constraints: `uq_<table>_<column>`

## Project Structure

```
fi.unfinitas.bookora
├── config/         # Configuration classes
├── controller/     # REST controllers
├── domain/         # Domain entities
├── repository/     # JPA repositories
├── service/        # Business logic
├── dto/            # Data Transfer Objects
├── mapper/         # MapStruct mappers
├── security/       # Security configuration
└── exception/      # Exception handling
```

## Development

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`
