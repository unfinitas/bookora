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
