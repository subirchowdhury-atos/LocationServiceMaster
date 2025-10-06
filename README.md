# LocationServiceMaster

A Spring Boot 3.5 application for address lookup and eligibility checking services. Converted from Ruby on Rails to Java 21.

## Overview

LocationServiceMaster provides RESTful APIs to:
- Look up and validate addresses using Google Maps API or mock data
- Check address eligibility based on configurable business rules
- Cache results in Redis for improved performance
- Secure API endpoints with token-based authentication

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.5
- **Build Tool**: Maven
- **Cache**: Redis
- **Security**: Spring Security with custom token authentication
- **External APIs**: Google Maps Geocoding API

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- Redis server (for caching)
- Google Maps API key (optional, for production address lookup)

## Configuration

### Application Properties

Create `application.yml` in `src/main/resources`:

```yaml
server:
  port: 8080

# API Security
api:
  security:
    enabled: true
    tokens: ${API_TOKENS:00000-000-00000,11111-111-11111}
    public-paths: /api/actuator/**,/api/v1/address/health,/api/swagger-ui/**,/api/v3/api-docs/**

# Google Maps
google:
  maps:
    enabled: ${USE_GOOGLE_MAPS_API:false}
    api:
      key: ${GOOGLE_MAPS_API_KEY:}

# Redis Configuration
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  password: ${REDIS_PASSWORD:}

# Cache Settings
eligibility:
  rules:
    cache-duration: 3600

# Address Fixtures
address:
  fixtures:
    path: classpath:fixtures/addresses.json
```

### Environment Variables

Set the following environment variables:

```bash
export API_TOKENS=your-api-token-1,your-api-token-2
export GOOGLE_MAPS_API_KEY=your-google-maps-api-key
export USE_GOOGLE_MAPS_API=true
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### Mock Data Setup

For development/testing without Google Maps API, create `src/main/resources/fixtures/addresses.json`:

```json
{
  "123 Main St, Alameda, CA 90255": {
    "street": "123 Main St",
    "city": "Alameda",
    "state": "California",
    "zip": "90255",
    "country": "United States"
  },
  "456 Oak Ave, San Francisco, CA 94102": {
    "street": "456 Oak Ave",
    "city": "San Francisco",
    "state": "California",
    "zip": "94102",
    "country": "United States"
  }
}
```

## Building the Project

```bash
# Clean and build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run tests only
mvn test
```

## Running the Application

```bash
# Using Maven
mvn spring-boot:run

# Using Java
java -jar target/LocationServiceMaster-1.0.0.jar
```

## API Endpoints

### 1. Address Eligibility Check (Ruby-compatible)

**Endpoint:** `POST /api/v1/address/eligibility_check`

**Headers:**
```
API-TOKEN: your-api-token
Content-Type: application/json
```

**Request Body:**
```json
{
  "address": "123 Main St, Alameda, CA 90255"
}
```

**Response (Eligible):**
```json
{
  "message": "address_eligible",
  "formatted_address": {
    "street": "123 Main St",
    "city": "Alameda",
    "state": "California",
    "zip": "90255",
    "country": "United States"
  }
}
```

**Response (Not Found):**
```json
{
  "message": "Address Not found"
}
```

### 2. Structured Eligibility Check

**Endpoint:** `POST /api/v1/address/check`

**Headers:**
```
API-TOKEN: your-api-token
Content-Type: application/json
```

**Request Body:**
```json
{
  "street_address": "123 Main St",
  "city": "Alameda",
  "state": "CA",
  "zip_code": "90255",
  "country": "USA"
}
```

**Response:**
```json
{
  "eligible": true,
  "message": "address_eligible",
  "formatted_address": {
    "street": "123 Main St",
    "city": "Alameda",
    "state": "California",
    "zip": "90255",
    "country": "United States"
  }
}
```

### 3. Health Check (No Authentication Required)

**Endpoint:** `GET /api/v1/address/health`

**Response:**
```json
{
  "status": "UP"
}
```

## API Authentication

All protected endpoints require an API token in the request header:

```
API-TOKEN: your-token-here
```

Or alternatively:
```
HTTP_API_TOKEN: your-token-here
```

Public endpoints (no authentication required):
- `/api/v1/address/health`
- `/api/actuator/**`
- `/api/swagger-ui/**`
- `/api/v3/api-docs/**`

## Eligibility Rules

Currently, addresses are eligible if they meet the following criteria:

1. **California addresses** in eligible cities:
   - Alameda
   - San Francisco
   - Oakland
   - Berkeley
   - San Jose

2. **Special case**: Alameda with zip code 90255

These rules are implemented in `PropertyEligibilityService` and can be customized as needed.

## Caching Strategy

The application implements two-layer caching:

1. **Address Lookup Cache** (`address:lookup:*`)
   - Caches Google Maps API responses
   - Reduces external API calls
   - TTL: Configurable (default 1 hour)

2. **Eligibility Cache** (`eligibility:*`)
   - Caches eligibility check results
   - Improves response time for repeated queries
   - TTL: Configurable (default 1 hour)

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ApiTokenAuthenticationFilterTest

# Run with coverage
mvn test jacoco:report
```

### Disabling Security for Development

Set in `application.yml`:
```yaml
api:
  security:
    enabled: false
```

Or via environment variable:
```bash
export API_SECURITY_ENABLED=false
```

### Using Mock Data Instead of Google Maps

Set in `application.yml`:
```yaml
google:
  maps:
    enabled: false
```

## Project Structure

```
src/main/java/com/locationservicemaster/
├── config/                     # Configuration classes
├── controller/                # REST controllers
├── dto/                       # Data Transfer Objects
├── security/                  # Security components
└── service/                   # Business logic
```

## Migration from Ruby on Rails

This application is a Java/Spring Boot conversion of the original Ruby on Rails LocationServiceMaster project. Key changes:

- **Language**: Ruby → Java 21
- **Framework**: Rails → Spring Boot 3.5
- **Build**: Bundler → Maven
- **HTTP Client**: RestClient → RestTemplate
- **Cache**: Rails.cache → Redis with Spring Data Redis
- **Authentication**: Custom token auth maintained, integrated with Spring Security

## Troubleshooting

### Redis Connection Issues

```bash
# Check if Redis is running
redis-cli ping

# Start Redis (macOS)
brew services start redis

# Start Redis (Linux)
sudo systemctl start redis
```

### Google Maps API Issues

- Verify API key is valid and has Geocoding API enabled
- Check API quota in Google Cloud Console
- Enable billing if required
- For development, use mock data instead

### Build Issues

```bash
# Clear Maven cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Rebuild with verbose output
mvn clean install -X
```