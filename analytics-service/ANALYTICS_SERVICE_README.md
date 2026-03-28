# 📊 Analytics Service - Admin Analytics Dashboard

**Status**: ✅ Backend Implementation Complete  
**Version**: 1.0.0  
**Author**: OTT Platform Team

---

## 🎯 Overview

Analytics Service là microservice chuyên biệt thu thập và phân tích dữ liệu người dùng cho Admin Dashboard của nền tảng OTT. Service tuân thủ nghiêm ngặt các nguyên tắc Microservices Architecture và Event Sourcing.

### ✨ Key Features

- **📈 User Growth Analytics**: Track registrations, growth rate
- **👥 Active Users Tracking**: DAU, MAU, current active users
- **🔄 Retention Analysis**: Day 1, 7, 30 retention cohorts
- **📱 Device Distribution**: Mobile, Desktop, Tablet, TV
- **🌍 Geographic Analytics**: Country and city distribution
- **🔐 Login Method Stats**: LOCAL, GOOGLE, OTP, QR_CODE
- **⚡ Real-time Metrics**: Redis-powered live counters

---

## 🏗️ Architecture

### Event Sourcing Pattern

```
User Service/Auth Service → RabbitMQ → Analytics Service
                                              ↓
                                    raw_events_log (Source of Truth)
                                              ↓
                                    Scheduled Jobs (Aggregation)
                                              ↓
                              Daily/Monthly Metrics Tables
```

**Critical Principle**: Analytics Service **NEVER** accesses other services' databases directly. All data flows through RabbitMQ events and is stored in `raw_events_log`.

### Technology Stack

- **Spring Boot 3.3.5** + Java 21
- **PostgreSQL** (with JSONB support)
- **Redis** (Caching + Idempotency)
- **RabbitMQ** (Event Streaming)
- **Flyway** (Database Migration)
- **ShedLock** (Distributed Job Coordination)
- **OpenFeign** (Inter-service Communication)

---

## 📁 Project Structure

```
analytics-service/
├── src/main/java/iuh/fit/se/analyticsservice/
│   ├── AnalyticsServiceApplication.java
│   ├── client/
│   │   ├── UserServiceClient.java          # OpenFeign client
│   │   └── AuthServiceClient.java
│   ├── config/
│   │   ├── RabbitMQConfig.java             # Queues, Exchanges
│   │   ├── RedisConfig.java                # Cache config
│   │   └── ShedLockConfig.java             # Distributed locking
│   ├── controller/
│   │   ├── UserAnalyticsController.java    # Public analytics API
│   │   ├── RealtimeMetricsController.java  # Real-time stats
│   │   └── AdminController.java            # Admin operations
│   ├── dto/
│   │   ├── event/                          # RabbitMQ event DTOs
│   │   │   ├── UserRegisteredEvent.java
│   │   │   ├── SessionCreatedEvent.java
│   │   │   └── UserLoginEvent.java
│   │   └── response/                       # API response DTOs
│   │       ├── ApiResponse.java
│   │       ├── DailyMetricsResponse.java
│   │       ├── MonthlyMetricsResponse.java
│   │       └── RetentionResponse.java
│   ├── entity/
│   │   ├── RawEventLog.java                # Event store (JSONB)
│   │   ├── DailyUserMetrics.java
│   │   ├── MonthlyUserMetrics.java
│   │   ├── RetentionCohort.java
│   │   ├── DeviceAnalytics.java
│   │   ├── GeographicAnalytics.java
│   │   └── LoginMethodAnalytics.java
│   ├── exception/
│   │   ├── ResourceNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   ├── repository/                         # JPA Repositories
│   │   ├── RawEventLogRepository.java
│   │   ├── DailyUserMetricsRepository.java
│   │   └── ... (7 repositories)
│   ├── scheduler/
│   │   └── MetricsScheduler.java           # Cron jobs with ShedLock
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   └── service/
│       ├── EventConsumerService.java       # RabbitMQ consumer
│       ├── MetricsAggregationService.java  # Batch aggregation
│       ├── RealtimeMetricsService.java     # Redis operations
│       └── UserAnalyticsService.java       # Business logic
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
│       └── V1__Initial_Schema.sql          # Flyway migration
├── Dockerfile
└── pom.xml
```

---

## 🔧 Configuration

### Environment Variables

```bash
# Database
ANALYTICS_DB_URL=jdbc:postgresql://localhost:5432/analytics_db
ANALYTICS_DB_USERNAME=postgres
ANALYTICS_DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Service URLs
USER_SERVICE_URL=http://localhost:8082
AUTH_SERVICE_URL=http://localhost:8081

# Security
INTERNAL_API_KEY=your-internal-api-key-change-in-production
JWT_SECRET=your-jwt-secret-key-at-least-512-bits
```

---

## 🚀 Getting Started

### 1. Prerequisites

```bash
# Install dependencies
- Java 21
- PostgreSQL 14+
- Redis 7+
- RabbitMQ 3.12+
- Maven 3.9+
```

### 2. Database Setup

```bash
# Create database
createdb analytics_db

# Flyway will auto-run migrations on startup
# See: src/main/resources/db/migration/V1__Initial_Schema.sql
```

### 3. Build & Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Or using JAR
java -jar target/analytics-service-1.0.0.jar
```

### 4. Docker

```bash
# Build image
docker build -t analytics-service:1.0.0 .

# Run container
docker run -p 8084:8084 \
  -e ANALYTICS_DB_URL=jdbc:postgresql://host.docker.internal:5432/analytics_db \
  -e REDIS_HOST=host.docker.internal \
  -e RABBITMQ_HOST=host.docker.internal \
  analytics-service:1.0.0
```

---

## 📡 API Endpoints

### Public Analytics APIs

```http
# User Growth
GET /api/analytics/growth?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {jwt_token}

# Active Users
GET /api/analytics/active-users?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {jwt_token}

# Retention Analysis
GET /api/analytics/retention?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {jwt_token}

# Device Distribution
GET /api/analytics/devices?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {jwt_token}

# Geographic Distribution
GET /api/analytics/geography?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {jwt_token}

# Login Methods
GET /api/analytics/login-methods?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {jwt_token}

# Dashboard Summary
GET /api/analytics/dashboard?days=30
Authorization: Bearer {jwt_token}
```

### Real-time Metrics APIs

```http
# Current Active Users
GET /api/analytics/realtime/active-users
Authorization: Bearer {jwt_token}

# Today's Metrics
GET /api/analytics/realtime/today
Authorization: Bearer {jwt_token}

# Today's Registrations
GET /api/analytics/realtime/registrations
Authorization: Bearer {jwt_token}

# Today's Logins
GET /api/analytics/realtime/logins
Authorization: Bearer {jwt_token}
```

### Admin APIs (Requires ROLE_ADMIN)

```http
# Manual Daily Calculation
POST /api/analytics/admin/calculate-daily?date=2024-01-01
Authorization: Bearer {admin_jwt_token}

# Manual Monthly Calculation
POST /api/analytics/admin/calculate-monthly?year=2024&month=1
Authorization: Bearer {admin_jwt_token}

# Manual Retention Update
POST /api/analytics/admin/update-retention?cohortDate=2024-01-01
Authorization: Bearer {admin_jwt_token}

# Reset Daily Counters
POST /api/analytics/admin/reset-counters
Authorization: Bearer {admin_jwt_token}

# System Health
GET /api/analytics/admin/health
Authorization: Bearer {admin_jwt_token}
```

---

## ⏰ Scheduled Jobs

All jobs use **ShedLock** to prevent duplicate execution across multiple instances.

| Job                       | Schedule                | Description                       |
| ------------------------- | ----------------------- | --------------------------------- |
| `calculateDailyMetrics`   | Daily at 1:00 AM        | Aggregate yesterday's metrics     |
| `calculateMonthlyMetrics` | 1st of month at 2:00 AM | Aggregate last month's metrics    |
| `updateRetentionCohorts`  | Daily at 3:00 AM        | Update retention for last 30 days |
| `updateDeviceAnalytics`   | Daily at 4:00 AM        | Aggregate device statistics       |
| `resetDailyCounters`      | Daily at 12:00 AM       | Reset Redis counters              |
| `cleanupOldEvents`        | Weekly (Sunday 5:00 AM) | Delete events older than 90 days  |
| `healthCheck`             | Every 5 minutes         | Monitor system health             |

---

## 🔐 Security

### Authentication Methods

1. **JWT Token**: For admin users accessing analytics
2. **Internal API Key**: For service-to-service communication

### Authorization

- **Public APIs**: Require valid JWT token
- **Admin APIs**: Require JWT with `ROLE_ADMIN`
- **Health Check**: Public (no auth)

---

## 🎯 Event Processing

### Idempotency Pattern

```java
// Check if event already processed (Redis TTL 24h)
if (isEventProcessed(eventId)) {
    log.warn("Event {} already processed, skipping", eventId);
    return;
}

// Process event...
rawEventLogRepository.save(rawEvent);

// Mark as processed
markEventAsProcessed(eventId);
```

### Event Types

1. **UserRegisteredEvent**: New user registration
2. **SessionCreatedEvent**: User creates new session
3. **UserLoginEvent**: User login attempt (success/failed)

---

## 📊 Database Schema

### Core Tables

- `raw_events_log`: Event store (JSONB payload)
- `daily_user_metrics`: Daily aggregated metrics
- `monthly_user_metrics`: Monthly aggregated metrics
- `retention_cohort`: Retention analysis per cohort
- `device_analytics`: Device type distribution
- `geographic_analytics`: Country/city distribution
- `login_method_analytics`: Login method statistics
- `shedlock`: Distributed job coordination

See: `src/main/resources/db/migration/V1__Initial_Schema.sql`

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Integration tests
mvn verify
```

---

## 📈 Monitoring

### Health Check

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

### Metrics

```http
GET /actuator/metrics
GET /actuator/prometheus
```

---

## 🔄 Integration with Other Services

### Required Changes

#### 1. User Service - Publish Events

See: `INTEGRATION_GUIDE.md` for detailed instructions

#### 2. Auth Service - Publish Events

See: `INTEGRATION_GUIDE.md` for detailed instructions

#### 3. API Gateway - Route Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: analytics-service
          uri: http://analytics-service:8084
          predicates:
            - Path=/api/analytics/**
```

---

## 🐛 Troubleshooting

### Issue: Events not being consumed

```bash
# Check RabbitMQ queues
rabbitmqctl list_queues

# Check Analytics Service logs
docker logs analytics-service | grep "RabbitMQ"
```

### Issue: Daily metrics not calculating

```bash
# Check ShedLock table
SELECT * FROM shedlock;

# Manually trigger calculation (Admin API)
POST /api/analytics/admin/calculate-daily?date=2024-01-01
```

### Issue: Redis cache not working

```bash
# Test Redis connection
redis-cli ping

# Check cache health
GET /api/analytics/admin/health
```

---

## 📝 Notes

### Production Checklist

- [ ] Change `INTERNAL_API_KEY` to strong secret
- [ ] Change `JWT_SECRET` to 512-bit key
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure proper connection pool sizes
- [ ] Set up monitoring alerts
- [ ] Configure log aggregation
- [ ] Enable SSL/TLS for Redis/RabbitMQ
- [ ] Set up database backups
- [ ] Configure resource limits in Kubernetes

---

## 📞 Contact

**Team**: OTT Platform Development  
**Project**: Admin Analytics Dashboard  
**Repository**: ott-backend/analytics-service

---

**Last Updated**: 2024-01-01  
**Build Status**: ✅ Passing
