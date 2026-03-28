# Analytics Service

Analytics Service for OTT Platform - Provides user analytics and metrics for admin dashboard.

## Features

- **User Growth Analytics**: Track new registrations, total users, growth trends
- **Active Users Metrics**: Daily Active Users (DAU), Monthly Active Users (MAU)
- **Retention Analysis**: Cohort-based retention tracking (1-day, 7-day, 30-day, 60-day, 90-day)
- **Device Analytics**: Distribution by device type (Mobile, Desktop, Tablet, TV)
- **Geographic Analytics**: User distribution by country and city
- **Login Analytics**: Login success/failure rates by method
- **Real-time Monitoring**: Active users, live registrations, login events

## Architecture

### Event-Driven Design
- Consumes events from RabbitMQ (user registrations, logins, sessions)
- Stores raw events in PostgreSQL `raw_events_log` table for batch processing
- **Event Sourcing Pattern**: Single source of truth, no direct database queries to other services
- Real-time metrics cached in Redis

### Batch Processing
- Scheduled jobs run daily to aggregate metrics from `raw_events_log`
- **ShedLock** ensures single execution in distributed environment (prevents duplicate processing)
- **Flyway** manages database migrations (no ddl-auto=update)

### Idempotency
- All RabbitMQ events include `eventId` (UUID)
- Redis-based deduplication check before processing
- TTL 24 hours to prevent re-processing

## Technology Stack

- **Framework**: Spring Boot 3.3.5
- **Language**: Java 21
- **Database**: PostgreSQL (analytics_db)
- **Cache**: Redis
- **Message Queue**: RabbitMQ
- **Migration**: Flyway
- **Distributed Lock**: ShedLock 5.10.0
- **API Client**: OpenFeign

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- RabbitMQ 3.12+

## Environment Variables

```bash
# Database
ANALYTICS_DB_URL=jdbc:postgresql://localhost:5432/analytics_db
ANALYTICS_DB_USERNAME=postgres
ANALYTICS_DB_PASSWORD=your-password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=your-rabbitmq-password

# Service URLs
USER_SERVICE_URL=http://localhost:8082
AUTH_SERVICE_URL=http://localhost:8081

# Security
INTERNAL_API_KEY=your-internal-api-key
JWT_SECRET=your-jwt-secret-at-least-512-bits

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Build & Run

### Local Development
```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

### Docker
```bash
# Build image
docker build -t analytics-service:latest .

# Run container
docker run -p 8084:8084 \
  -e ANALYTICS_DB_URL=jdbc:postgresql://host.docker.internal:5432/analytics_db \
  -e REDIS_HOST=host.docker.internal \
  -e RABBITMQ_HOST=host.docker.internal \
  analytics-service:latest
```

### Docker Compose
```bash
docker-compose up -d analytics-service
```

## API Endpoints

### Analytics APIs (Admin Only)
```
GET /analytics/user-growth?startDate=2024-01-01&endDate=2024-12-31
GET /analytics/active-users?period=daily|monthly
GET /analytics/retention?cohortDate=2024-01-01
GET /analytics/device-distribution?startDate=2024-01-01
GET /analytics/geographic-distribution?startDate=2024-01-01
GET /analytics/login-methods?startDate=2024-01-01
GET /analytics/dashboard
```

### Real-time Stream (SSE)
```
GET /analytics/realtime/stream
```

### Health Check
```
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

## Database Schema

- **raw_events_log** - Event store for RabbitMQ events (JSONB payload)
- **daily_user_metrics** - Daily aggregated metrics
- **monthly_user_metrics** - Monthly aggregated metrics
- **retention_cohort** - Cohort retention tracking
- **device_analytics** - Device type statistics
- **geographic_analytics** - Geographic distribution
- **login_method_analytics** - Login method statistics
- **shedlock** - Distributed lock table

## Scheduled Jobs

All jobs use `@SchedulerLock` to prevent duplicate execution:

- **Daily Metrics**: 1:00 AM - Calculate yesterday's metrics from `raw_events_log`
- **Monthly Metrics**: 2:00 AM (1st of month) - Calculate previous month from `daily_user_metrics`
- **Retention Cohorts**: 3:00 AM - Update retention rates from `raw_events_log`
- **Device Analytics**: 1:30 AM - Aggregate device statistics from `raw_events_log`
- **Geographic Analytics**: 1:45 AM - Aggregate geographic data from `raw_events_log`
- **Cache Update**: Every 1 minute - Update real-time metrics in Redis

## Security

- Admin endpoints require JWT with `ADMIN` role
- Internal service calls use `X-Internal-Key` header
- Non-root user in Docker container
- CORS configured for specified origins

## Monitoring

- Spring Boot Actuator endpoints
- Prometheus metrics exposed at `/actuator/prometheus`
- Health checks for liveness and readiness probes
- Structured logging with correlation IDs

## Key Design Decisions

### 1. Event Sourcing (NOT Direct Database Access)
✅ Events stored in `raw_events_log` table  
✅ Batch jobs query own database only  
❌ NO direct queries to User Service or Auth Service databases  

### 2. ShedLock for Distributed Scheduling
✅ Prevents duplicate job execution when scaled horizontally  
✅ Uses database-based locking mechanism  

### 3. Idempotency for RabbitMQ
✅ All events have unique `eventId`  
✅ Redis check before processing  
✅ Handles at-least-once delivery semantics  

### 4. Flyway Database Migrations
✅ Version-controlled schema changes  
✅ `ddl-auto=validate` (not update)  
✅ Safe for production deployments  

## Port

- **8084**: Main application port

## Author

OTT Platform Team

## Version

1.0.0-SNAPSHOT
