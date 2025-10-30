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
        string username UK "uq_user_username"
        string first_name
        string last_name
        string email UK "uq_user_email"
        string password "nullable for guest users"
        string role "USER, PROVIDER, ADMIN"
        boolean is_guest
        string phone_number
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
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
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
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
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
    }

    t_service {
        bigint id PK
        uuid provider_id FK
        string name
        text description
        integer duration_minutes
        decimal price
        boolean is_active
        bigint version "optimistic locking"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
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
        bigint version "optimistic locking"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
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
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
    }

    t_guest_access_token {
        bigint id PK
        bigint booking_id FK,UK "uq_guest_access_token_booking_id"
        uuid token UK "uq_guest_access_token_token"
        timestamp expires_at
        timestamp confirmed_at
        bigint version "optimistic locking"
        timestamp created_at
        timestamp updated_at
        timestamp deleted_at "soft delete"
        uuid deleted_by "soft delete"
    }
```

### Key Relationships

- **User-Provider**: One-to-one relationship. A user can become a provider.
- **User-Booking**: One-to-many. A customer (user) can make multiple bookings.
- **Provider-Service**: One-to-many. A provider offers multiple services.
- **Provider-Booking**: One-to-many. A provider receives multiple bookings.
- **Provider-Availability**: One-to-many. A provider defines multiple availability time slots.
- **Provider-Address**: Many-to-one. Multiple providers can share the same address (co-working spaces, business centers).
- **Service-Booking**: One-to-many. A service can be booked multiple times.
- **Booking-GuestAccessToken**: One-to-one. Each booking can have a guest access token for guest users.

### Schema Features

- **Soft Deletes**: All entities support soft deletion using `deleted_at` and `deleted_by` columns
  - Enables data retention and audit trail
  - @SQLRestriction automatically filters deleted records in queries
- **Optimistic Locking**: `version` column on `t_booking`, `t_service`, and `t_guest_access_token`
  - Prevents lost updates in concurrent scenarios
  - Uses @Version annotation for JPA-managed versioning
- **Guest User Support**:
  - `is_guest` flag distinguishes guest vs registered users
  - Guest users have nullable `password` field
  - Database constraint ensures guests cannot have passwords
- **Booking Conflict Prevention**:
  - Temporal range exclusion constraint prevents overlapping provider bookings
  - Customer exclusion constraint prevents double-booking for customers
  - Application-level + database-level validation for data integrity
- **Address Reusability**: Separate entity table for sharing addresses
  - Multiple providers can share the same address (co-working spaces, business centers)
  - Enables address history tracking via audit timestamps
  - Supports efficient location-based queries with indexed city/country columns

### Enums

- **UserRole**: `USER`, `PROVIDER`, `ADMIN`
- **BookingStatus**: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`

### Naming Conventions

- Tables: `t_<entity>` prefix
- Columns: `snake_case`
- Boolean columns: `is_<name>` prefix
- Timestamp columns: `<action>_at` suffix
- Unique constraints: `uq_<table>_<column>`

## Project Structure

```
src/main/java/fi/unfinitas/bookora
├── config
├── controller
├── domain
├── dto
├── exception
├── mapper
├── repository
├── security
├── service

src/main/resources
├── application.yml
└── db/migration
```

## Development

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`
