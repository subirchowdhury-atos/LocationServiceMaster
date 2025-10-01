# Location Service Master

A Spring Boot 3.5 microservice for checking address eligibility based on configurable zones and business rules. This service was converted from a Ruby on Rails application to Java/Spring Boot.

## Technology Stack

- **Java 21**
- **Spring Boot 3.5**
- **Spring Data JPA**
- **PostgreSQL / H2 (for testing)**
- **Redis (for caching)**
- **Docker & Docker Compose**
- **Maven**
- **OpenAPI/Swagger Documentation**

## Project Structure

```
src/main/java/com/locationservicemaster/
├── config/                      # Spring configuration classes
│   ├── ApiVersionConfig.java   # API versioning configuration
│   ├── RedisConfig.java        # Redis and cache configuration
│   └── WebConfig.java          # Web MVC and CORS configuration
├── controller/                  # REST API controllers
│   └── AddressEligibilityController.java
├── domain/entity/              # JPA entities
│   ├── Address.java           # Address entity
│   └── EligibilityZone.java  # Eligibility zone configuration
├── dto/                        # Data Transfer Objects
│   ├── AddressEligibilityRequest.java
│   └── AddressEligibilityResponse.java
├── repository/                 # JPA repositories
│   ├── AddressRepository.java
│   └── EligibilityZoneRepository.java
├── service/                    # Business logic services
│   ├── AddressEligibilityService.java
│   ├── CacheService.java